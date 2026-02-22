// TEMPORARY: Routes to HuggingFace for the R1-Distill model, DeepSeek for everything else.
// To delete HF support: remove this file and HuggingFaceChatRepositoryImpl.kt,
// then bind ChatRepository directly to ChatRepositoryImpl in AppModule.kt.
package com.example.aichallengeapp.data.repository

import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.domain.model.ChatResult
import com.example.aichallengeapp.domain.repository.ChatRepository

private const val HF_ROUTED_MODEL_ID = "deepseek-r1-distill-qwen-7b"

class HuggingFaceRoutingChatRepository(
    private val deepSeekRepository: ChatRepository,
    private val huggingFaceRepository: ChatRepository
) : ChatRepository {

    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?,
        model: String
    ): Result<ChatResult> {
        return if (model == HF_ROUTED_MODEL_ID) {
            huggingFaceRepository.sendMessage(messages, maxTokens, temperature, model)
        } else {
            deepSeekRepository.sendMessage(messages, maxTokens, temperature, model)
        }
    }
}
