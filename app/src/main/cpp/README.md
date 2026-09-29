# Native AI (llama.cpp)

On-device inference uses [llama.cpp](https://github.com/ggml-org/llama.cpp) via JNI.

## Setup

```bash
git clone --depth 1 https://github.com/ggml-org/llama.cpp.git app/src/main/cpp/llama.cpp
```

Then build the app normally. Only **arm64-v8a** is supported for AI.

## Model

Download a GGUF model from Settings → AI Assistant, or host your own URL in `AiModelCatalog.kt`.
