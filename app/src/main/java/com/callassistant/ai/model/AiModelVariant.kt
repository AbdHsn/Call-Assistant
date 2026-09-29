package com.callassistant.ai.model

enum class AiModelVariant(val id: String) {
    QWEN_2B("qwen3.5-2b"),
    QWEN_0_8B("qwen3.5-0.8b"),
    AZURE_OPENAI("azure_openai");

    val isOnDevice: Boolean get() = this != AZURE_OPENAI

    companion object {
        val DEFAULT = AZURE_OPENAI

        fun fromId(id: String): AiModelVariant = entries.find { it.id == id } ?: DEFAULT
    }
}

data class AiModelDefinition(
    val variant: AiModelVariant,
    val displayName: String,
    val subtitle: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val minRamGb: Double,
    val expectedSha256: String = ""
)

object AiModelCatalog {
    private val models = listOf(
        AiModelDefinition(
            variant = AiModelVariant.QWEN_2B,
            displayName = "Qwen3.5-2B",
            subtitle = "Best Bangla + English · ~1.5 GB · 6 GB+ RAM",
            fileName = "qwen3.5-2b-instruct-q4_k_m.gguf",
            downloadUrl =
                "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeBytes = 986_000_000L,
            minRamGb = 5.5
        ),
        AiModelDefinition(
            variant = AiModelVariant.QWEN_0_8B,
            displayName = "Qwen3.5-0.8B",
            subtitle = "Lighter for 4–6 GB phones · ~600 MB · 4 GB+ RAM",
            fileName = "qwen3.5-0.8b-instruct-q4_k_m.gguf",
            downloadUrl =
                "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            sizeBytes = 398_000_000L,
            minRamGb = 4.0
        ),
        AiModelDefinition(
            variant = AiModelVariant.AZURE_OPENAI,
            displayName = "Azure OpenAI",
            subtitle = "Cloud · gpt-5.4-mini · requires internet",
            fileName = "",
            downloadUrl = "",
            sizeBytes = 0L,
            minRamGb = 0.0
        )
    )

    val all: List<AiModelDefinition> = models

    fun get(variant: AiModelVariant): AiModelDefinition =
        models.first { it.variant == variant }

    fun recommendedForRam(ramGb: Double): AiModelVariant = when {
        ramGb >= get(AiModelVariant.QWEN_2B).minRamGb -> AiModelVariant.QWEN_2B
        ramGb >= get(AiModelVariant.QWEN_0_8B).minRamGb -> AiModelVariant.QWEN_0_8B
        else -> AiModelVariant.QWEN_0_8B
    }
}
