package com.flxrs.dankchat.ui.chat

import com.flxrs.dankchat.chat.ChatEncryption
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ChatEncryptionTest {
    @Test
    fun `round trip works for every encoding`() {
        ChatEncryption.Encoding.entries.forEach { encoding ->
            val encrypted = ChatEncryption.encrypt("nigga", "secret", encoding)
            assertEquals(expected = encoding, actual = ChatEncryption.detectEncoding(encrypted))
            assertEquals(expected = "nigga", actual = ChatEncryption.decrypt(encrypted, "secret", encoding))
        }
    }

    @Test
    fun `random spaces don't break detection or decryption`() {
        ChatEncryption.Encoding.entries.forEach { encoding ->
            val encrypted = ChatEncryption.addRandomSpaces(ChatEncryption.encrypt("wassap nigga", "secret", encoding)).trimEnd()
            assertEquals(expected = encoding, actual = ChatEncryption.detectEncoding(encrypted))
            assertEquals(expected = "wassap nigga", actual = ChatEncryption.decrypt(encrypted, "secret", encoding))
        }
    }

    @Test
    fun `regular chat messages are not detected as encrypted`() {
        listOf("hello", "deadbeef", "123", "", "   ", "привет", "こんにちは").forEach {
            assertNull(ChatEncryption.detectEncoding(it), it)
        }
    }
}
