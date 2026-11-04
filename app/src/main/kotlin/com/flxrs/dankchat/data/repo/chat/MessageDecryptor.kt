package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.chat.ChatEncryption
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger("MessageDecryptor")

@Single
class MessageDecryptor(
    private val messageProcessor: MessageProcessor,
    private val chatSettingsDataStore: ChatSettingsDataStore,
) {
    fun isCandidate(message: Message): Boolean {
        if (message !is PrivMessage || message.encrypted) return false
        if (!chatSettingsDataStore.current().enableMessageEncryption) return false
        return ChatEncryption.detectEncoding(message.originalMessage) != null
    }

    suspend fun decrypt(message: PrivMessage): Message? {
        val settings = chatSettingsDataStore.current()
        val encoding = ChatEncryption.detectEncoding(message.originalMessage) ?: return null
        val plainText =
            runCatching { ChatEncryption.decrypt(message.originalMessage, settings.encryptionPassword, encoding) }
                .onFailure { logger.debug { "Failed to decrypt message ${message.id}: ${it.message}" } }
                .getOrNull() ?: return null

        val decrypted =
            message.copy(
                message = plainText,
                originalMessage = plainText,
                encrypted = true,
                emotes = emptyList(),
                emoteData = message.emoteData.copy(message = plainText, emotesWithPositions = emptyList()),
            )
        return messageProcessor.reprocessContent(decrypted)
    }
}
