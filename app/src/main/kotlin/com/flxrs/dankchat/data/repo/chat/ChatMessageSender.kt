package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.chat.ChatEncryption
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.api.helix.HelixApiException
import com.flxrs.dankchat.data.api.helix.HelixError
import com.flxrs.dankchat.data.api.helix.dto.SendChatMessageRequestDto
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.twitch.message.SystemMessageType
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.developer.ChatSendProtocol
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import com.flxrs.dankchat.utils.extensions.INVISIBLE_CHAR
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger("ChatMessageSender")

@Single
class ChatMessageSender(
    private val chatConnector: ChatConnector,
    private val helixApiClient: HelixApiClient,
    private val channelRepository: ChannelRepository,
    private val authDataStore: AuthDataStore,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatEventProcessor: ChatEventProcessor,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val dispatchersProvider: DispatchersProvider,
) {
    suspend fun send(
        channel: UserName,
        message: String,
        replyId: String? = null,
        forceIrc: Boolean = false,
        encrypt: Boolean = false,
    ) {
        if (message.isBlank()) {
            return
        }

        val protocol = developerSettingsDataStore.current().chatSendProtocol
        when {
            forceIrc || protocol == ChatSendProtocol.IRC -> sendViaIrc(channel, message, replyId, encrypt)
            else -> sendViaHelix(channel, message, replyId, encrypt)
        }
    }

    private suspend fun sendViaIrc(
        channel: UserName,
        message: String,
        replyId: String?,
        encrypt: Boolean,
    ) {
        val trimmedMessage = message.trimEnd()
        val replyIdOrBlank = replyId?.let { "@reply-parent-msg-id=$it " }.orEmpty()
        val messageWithSuffix = prepareWireMessage(channel, trimmedMessage, encrypt)

        chatEventProcessor.setLastMessage(channel, sent = messageWithSuffix, typed = trimmedMessage)
        chatConnector.sendRaw("${replyIdOrBlank}PRIVMSG #$channel :$messageWithSuffix")
        chatMessageRepository.incrementSentMessageCount(ChatSendProtocol.IRC)
    }

    private suspend fun sendViaHelix(
        channel: UserName,
        message: String,
        replyId: String?,
        encrypt: Boolean,
    ) {
        val trimmedMessage = message.trimEnd()
        val senderId =
            authDataStore.userIdString ?: run {
                postError(channel, SystemMessageType.SendNotLoggedIn)
                return
            }
        val broadcasterId =
            channelRepository.getChannel(channel)?.id ?: run {
                postError(channel, SystemMessageType.SendChannelNotResolved(channel))
                return
            }

        val messageWithSuffix = prepareWireMessage(channel, trimmedMessage, encrypt)
        val request =
            SendChatMessageRequestDto(
                broadcasterId = broadcasterId,
                senderId = senderId,
                message = messageWithSuffix,
                replyParentMessageId = replyId,
            )

        helixApiClient.postChatMessage(request).fold(
            onSuccess = { response ->
                when {
                    response.isSent -> {
                        chatEventProcessor.setLastMessage(channel, sent = messageWithSuffix, typed = trimmedMessage)
                        chatMessageRepository.incrementSentMessageCount(ChatSendProtocol.Helix)
                    }

                    else -> {
                        val type =
                            when (val reason = response.dropReason) {
                                null -> SystemMessageType.SendNotDelivered
                                else -> SystemMessageType.SendDropped(reason.message, reason.code)
                            }
                        postError(channel, type)
                    }
                }
            },
            onFailure = { throwable ->
                logger.error(throwable) { "Helix send failed" }
                postError(channel, throwable.toSendErrorType())
            },
        )
    }

    private suspend fun prepareWireMessage(
        channel: UserName,
        trimmedMessage: String,
        encrypt: Boolean,
    ): String = when {
        // A random IV makes every ciphertext unique, so no duplicate bypass is needed
        encrypt -> encryptMessage(trimmedMessage)
        else -> bypassDuplicateIfNeeded(channel, trimmedMessage)
    }

    private suspend fun encryptMessage(message: String): String = withContext(dispatchersProvider.default) {
        val settings = chatSettingsDataStore.current()
        val encrypted = ChatEncryption.encrypt(message, settings.encryptionPassword, settings.encryptionEncoding)
        when {
            settings.randomEncryptedSpaces -> ChatEncryption.addRandomSpaces(encrypted).trimEnd()
            else -> encrypted
        }
    }

    // When the user repeats the same typed message, compound the bypass on the previously-sent
    // wire so each successive send is unique within Twitch's duplicate-detection window.
    private fun bypassDuplicateIfNeeded(
        channel: UserName,
        trimmedMessage: String,
    ): String {
        val previousTypedMessage = chatEventProcessor.getLastMessageForDisplay(channel)
        val previousSentMessage = chatEventProcessor.getLastMessage(channel)
        return when {
            previousTypedMessage == trimmedMessage && previousSentMessage != null -> applyAntiDuplicate(previousSentMessage)
            else -> trimmedMessage
        }
    }

    private fun postError(
        channel: UserName,
        type: SystemMessageType,
    ) {
        chatMessageRepository.addSystemMessage(channel, type)
        chatMessageRepository.incrementSendFailureCount()
    }

    private fun applyAntiDuplicate(message: String): String {
        val startIndex =
            when {
                message.startsWith('/') || message.startsWith('.') -> message.indexOf(' ').let { if (it == -1) 0 else it + 1 }
                else -> 0
            }
        val spaceIndex = message.indexOf(' ', startIndex)

        return when {
            spaceIndex != -1 -> message.replaceRange(spaceIndex, spaceIndex + 1, "  ")
            else -> "$message $INVISIBLE_CHAR"
        }
    }

    private fun Throwable.toSendErrorType(): SystemMessageType = when (this) {
        is HelixApiException -> {
            when (error) {
                HelixError.NotLoggedIn -> SystemMessageType.SendNotLoggedIn
                HelixError.MissingScopes -> SystemMessageType.SendMissingScopes
                HelixError.UserNotAuthorized -> SystemMessageType.SendNotAuthorized
                HelixError.MessageTooLarge -> SystemMessageType.SendMessageTooLarge
                HelixError.ChatMessageRateLimited -> SystemMessageType.SendRateLimited
                else -> SystemMessageType.SendFailed(message)
            }
        }

        else -> {
            SystemMessageType.SendFailed(message)
        }
    }
}
