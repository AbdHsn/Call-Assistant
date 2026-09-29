#include <jni.h>
#include <atomic>
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
#include <unistd.h>

#include "ggml-backend.h"
#include "llama.h"
#include "common.h"
#include "sampling.h"
#include "logging.h"

static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static common_sampler *g_sampler = nullptr;
static llama_batch g_batch{};
static bool g_batch_initialized = false;
static std::atomic<bool> g_cancel{false};

static constexpr int kContextSize = 4096;
static constexpr int kBatchSize = 512;
static constexpr int kMaxThreads = 4;

static bool file_starts_with_gguf(const char * path) {
    FILE * file = fopen(path, "rb");
    if (!file) {
        return false;
    }
    char magic[4] = {};
    const size_t read = fread(magic, 1, 4, file);
    fclose(file);
    return read == 4 && std::memcmp(magic, "GGUF", 4) == 0;
}

static void free_batch() {
    if (g_batch_initialized) {
        llama_batch_free(g_batch);
        g_batch_initialized = false;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_callassistant_ai_engine_LlamaNative_nativeInit(JNIEnv *, jclass, jstring) {
    llama_log_set(android_ggml_log_callback, nullptr);
    llama_backend_init();
    const size_t backend_count = ggml_backend_reg_count();
    LOGi("llama backend initialized with %zu backend(s)", backend_count);
    return backend_count == 0 ? 1 : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_callassistant_ai_engine_LlamaNative_nativeLoad(JNIEnv *env, jclass, jstring jModelPath) {
    g_cancel = false;
    if (g_sampler) {
        common_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    free_batch();
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }

    const char *path = env->GetStringUTFChars(jModelPath, nullptr);
    LOGi("Loading model from %s", path);

    if (!file_starts_with_gguf(path)) {
        LOGe("Model file is missing or is not a valid GGUF file");
        env->ReleaseStringUTFChars(jModelPath, path);
        return 4;
    }

    if (ggml_backend_reg_count() == 0) {
        LOGe("No GGML backends loaded; call nativeInit first");
        env->ReleaseStringUTFChars(jModelPath, path);
        return 5;
    }

    g_model = llama_model_load_from_file(path, llama_model_default_params());
    env->ReleaseStringUTFChars(jModelPath, path);
    if (!g_model) {
        LOGe("Model load failed");
        return 1;
    }

    llama_context_params ctx_params = llama_context_default_params();
    const int n_threads = std::max(
        2,
        std::min(kMaxThreads, (int) sysconf(_SC_NPROCESSORS_ONLN) - 1)
    );
    ctx_params.n_ctx = kContextSize;
    ctx_params.n_batch = kBatchSize;
    ctx_params.n_ubatch = kBatchSize;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;

    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGe("Context init failed");
        llama_model_free(g_model);
        g_model = nullptr;
        return 2;
    }

    g_batch = llama_batch_init(kBatchSize, 0, 1);
    g_batch_initialized = true;

    common_params_sampling sparams;
    sparams.temp = 0.7f;
    sparams.top_p = 0.9f;
    g_sampler = common_sampler_init(g_model, sparams);
    if (!g_sampler) {
        LOGe("Sampler init failed");
        return 3;
    }

    return 0;
}

static int decode_prompt(const std::vector<llama_token> &tokens) {
    llama_memory_clear(llama_get_memory(g_ctx), false);
    for (size_t i = 0; i < tokens.size(); i += kBatchSize) {
        const int cur = (int) std::min(tokens.size() - i, (size_t) kBatchSize);
        common_batch_clear(g_batch);
        for (int j = 0; j < cur; j++) {
            const bool want_logit = (i + (size_t) j == tokens.size() - 1);
            common_batch_add(g_batch, tokens[i + j], (llama_pos) (i + j), {0}, want_logit);
        }
        if (llama_decode(g_ctx, g_batch) != 0) {
            return 1;
        }
    }
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_callassistant_ai_engine_LlamaNative_nativeComplete(
    JNIEnv *env,
    jclass,
    jstring jPrompt,
    jint maxTokens
) {
    if (!g_ctx || !g_model || !g_sampler) {
        return env->NewStringUTF("");
    }

    g_cancel = false;
    common_sampler_reset(g_sampler);

    const char *prompt = env->GetStringUTFChars(jPrompt, nullptr);
    const auto tokens = common_tokenize(g_ctx, std::string(prompt), true, true);
    env->ReleaseStringUTFChars(jPrompt, prompt);

    if (tokens.empty() || decode_prompt(tokens) != 0) {
        LOGe("Prompt tokenization/decode failed");
        return env->NewStringUTF("");
    }

    std::string result;
    llama_pos pos = (llama_pos) tokens.size();
    const int limit = std::max(1, (int) maxTokens);

    for (int i = 0; i < limit && !g_cancel; i++) {
        const llama_token id = common_sampler_sample(g_sampler, g_ctx, -1);
        common_sampler_accept(g_sampler, id, true);

        if (llama_vocab_is_eog(llama_model_get_vocab(g_model), id)) {
            break;
        }

        result += common_token_to_piece(g_ctx, id);

        common_batch_clear(g_batch);
        common_batch_add(g_batch, id, pos++, {0}, true);
        if (llama_decode(g_ctx, g_batch) != 0) {
            break;
        }
    }

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_callassistant_ai_engine_LlamaNative_nativeCancel(JNIEnv *, jclass) {
    g_cancel = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_callassistant_ai_engine_LlamaNative_nativeUnload(JNIEnv *, jclass) {
    g_cancel = false;
    if (g_sampler) {
        common_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    free_batch();
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_callassistant_ai_engine_LlamaNative_nativeShutdown(JNIEnv *, jclass) {
    Java_com_callassistant_ai_engine_LlamaNative_nativeUnload(nullptr, nullptr);
    llama_backend_free();
}
