package com.flxrs.dankchat.preferences.chat

import com.flxrs.dankchat.chat.ChatEncryption
import com.flxrs.dankchat.data.twitch.badge.BadgeType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.format.DateTimeFormatter
import kotlin.uuid.Uuid

@Serializable
data class ChatSettings(
    val suggestionTypes: List<SuggestionType> = SuggestionType.DEFAULT,
    val suggestionMode: SuggestionMode = SuggestionMode.Automatic,
    val suggestionsMigrated: Boolean = false,
    @Deprecated("Migrated to suggestionTypes") val suggestions: Boolean = true,
    @Deprecated("Migrated to suggestionTypes") val supibotSuggestions: Boolean = false,
    val customCommands: List<CustomCommand> = emptyList(),
    val animateGifs: Boolean = true,
    val scrollbackLength: Int = 500,
    val showUsernames: Boolean = true,
    val userLongClickBehavior: UserLongClickBehavior = UserLongClickBehavior.MentionsUser,
    val colorizeNicknames: Boolean = true,
    val showTimedOutMessages: Boolean = true,
    val showTimestamps: Boolean = true,
    val timestampFormat: String = DEFAULT_TIMESTAMP_FORMAT,
    val visibleBadges: List<VisibleBadges> = VisibleBadges.entries,
    val visibleEmotes: List<VisibleThirdPartyEmotes> = VisibleThirdPartyEmotes.entries,
    val allowUnlistedSevenTvEmotes: Boolean = false,
    val sevenTVLiveEmoteUpdates: Boolean = true,
    @Deprecated("Migrated to BatterySettings.pauseSevenTvLiveUpdates") val sevenTVLiveEmoteUpdatesBehavior: LiveUpdatesBackgroundBehavior = LiveUpdatesBackgroundBehavior.FiveMinutes,
    val loadMessageHistory: Boolean = true,
    val loadMessageHistoryOnReconnect: Boolean = true,
    val showChatModes: Boolean = true,
    val alwaysShowPinnedMessage: Boolean = false,
    val showStreamTitleInLiveMessage: Boolean = false,
    val sharedChatMigration: Boolean = false,
    val enableMessageEncryption: Boolean = false,
    val encryptOnSend: Boolean = false,
    val randomEncryptedSpaces: Boolean = false,
    val encryptionPassword: String = ChatEncryption.DEFAULT_PASSWORD,
    val encryptionEncoding: ChatEncryption.Encoding = ChatEncryption.Encoding.Hebrew,
    val tinyInstances: List<TinyInstance> = emptyList()
) {
    @Transient
    val visibleBadgeTypes = visibleBadges.map { BadgeType.entries[it.ordinal] }

    @Transient
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(timestampFormat)

    companion object {
        private const val DEFAULT_TIMESTAMP_FORMAT = "HH:mm"
    }
}

@Serializable
data class CustomCommand(
    val trigger: String,
    val command: String,
    @Transient val id: String = Uuid.random().toString(),
)

@Serializable
enum class SuggestionType {
    Emotes,
    Users,
    Commands,
    SupibotCommands,
    ;

    companion object {
        val DEFAULT = listOf(Emotes, Users, Commands)
    }
}

@Serializable
data class TinyInstance(@Transient val id: String = Uuid.random().toString(), val url: String, val channelEmotes: Boolean, val globalEmotes: Boolean)

enum class UserLongClickBehavior {
    MentionsUser,
    OpensPopup,
}

enum class VisibleBadges {
    Authority,
    Predictions,
    Channel,
    Subscriber,
    Vanity,
    DankChat,
    SharedChat,
}

enum class VisibleThirdPartyEmotes {
    FFZ,
    BTTV,
    SevenTV,
}

@Serializable
enum class SuggestionMode {
    Automatic,
    PrefixOnly,
}

enum class LiveUpdatesBackgroundBehavior {
    Never,
    OneMinute,
    FiveMinutes,
    ThirtyMinutes,
    OneHour,
    Always,
}
