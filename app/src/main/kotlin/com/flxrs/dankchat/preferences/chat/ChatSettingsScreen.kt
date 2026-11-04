package com.flxrs.dankchat.preferences.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.components.ExpandablePreferenceItem
import com.flxrs.dankchat.chat.ChatEncryption
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.PreferenceCategory
import com.flxrs.dankchat.preferences.components.PreferenceItem
import com.flxrs.dankchat.preferences.components.PreferenceListDialog
import com.flxrs.dankchat.preferences.components.PreferenceMultiListDialog
import com.flxrs.dankchat.preferences.components.SliderPreferenceItem
import com.flxrs.dankchat.preferences.components.SwitchPreferenceItem
import com.flxrs.dankchat.utils.extensions.dp
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun ChatSettingsScreen(
    onNavToCommands: () -> Unit,
    onNavToUserDisplays: () -> Unit,
    onNavToBattery: () -> Unit,
    onNavToInstances: () -> Unit,
    onNavBack: () -> Unit,
) {
    val viewModel = koinViewModel<ChatSettingsViewModel>()
    val settings = viewModel.settings.collectAsStateWithLifecycle().value
    val restartRequiredTitle = stringResource(R.string.restart_required)
    val restartRequiredAction = stringResource(R.string.restart)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest {
            when (it) {
                ChatSettingsEvent.RestartRequired -> {
                    val result = snackbarHostState.showSnackbar(
                        message = restartRequiredTitle,
                        actionLabel = restartRequiredAction,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        ProcessPhoenix.triggerRebirth(context)
                    }
                }
            }
        }
    }

    ChatSettingsScreen(
        settings = settings,
        snackbarHostState = snackbarHostState,
        onInteraction = { viewModel.onInteraction(it) },
        onNavToCommands = onNavToCommands,
        onNavToUserDisplays = onNavToUserDisplays,
        onNavToBattery = onNavToBattery,
        onNavToInstances = onNavToInstances,
        onNavBack = onNavBack,
    )
}

@Composable
private fun ChatSettingsScreen(
    settings: ChatSettingsState,
    snackbarHostState: SnackbarHostState,
    onInteraction: (ChatSettingsInteraction) -> Unit,
    onNavToCommands: () -> Unit,
    onNavToUserDisplays: () -> Unit,
    onNavToBattery: () -> Unit,
    onNavToInstances: () -> Unit,
    onNavBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.preference_chat_header)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavBack,
                        content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            SuggestionsCategory(
                suggestionTypes = settings.suggestionTypes,
                suggestionMode = settings.suggestionMode,
                onNavToCommands = onNavToCommands,
                onInteraction = onInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            MessagesCategory(
                scrollbackLength = settings.scrollbackLength,
                showTimedOutMessages = settings.showTimedOutMessages,
                showTimestamps = settings.showTimestamps,
                timestampFormat = settings.timestampFormat,
                onInteraction = onInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            UsersCategory(
                showUsernames = settings.showUsernames,
                userLongClickBehavior = settings.userLongClickBehavior,
                colorizeNicknames = settings.colorizeNicknames,
                onNavToUserDisplays = onNavToUserDisplays,
                onInteraction = onInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            EmotesAndBadgesCategory(
                animateGifs = settings.animateGifs,
                visibleBadges = settings.visibleBadges,
                visibleEmotes = settings.visibleEmotes,
                onInteraction = onInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            SevenTVCategory(
                enabled = VisibleThirdPartyEmotes.SevenTV in settings.visibleEmotes,
                allowUnlistedSevenTvEmotes = settings.allowUnlistedSevenTvEmotes,
                sevenTVLiveEmoteUpdates = settings.sevenTVLiveEmoteUpdates,
                onInteraction = onInteraction,
                onNavToBattery = onNavToBattery,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            TinyCategory(
                onInteraction = onInteraction,
                onNavToInstances = onNavToInstances
            )
            HorizontalDivider(thickness = Dp.Hairline)
            MessageHistoryCategory(
                loadMessageHistory = settings.loadMessageHistory,
                loadMessageHistoryAfterReconnect = settings.loadMessageHistoryAfterReconnect,
                messageHistoryDashboardUrl = settings.messageHistoryDashboardUrl,
                onInteraction = onInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            ChannelDataCategory(
                showChatModes = settings.showChatModes,
                alwaysShowPinnedMessage = settings.alwaysShowPinnedMessage,
                showStreamTitleInLiveMessage = settings.showStreamTitleInLiveMessage,
                onInteraction = onInteraction,
            )
            HorizontalDivider(thickness = Dp.Hairline)
            EncryptionCategory(
                enableMessageEncryption = settings.enableMessageEncryption,
                randomSpaces = settings.randomEncryptedSpaces,
                encryptionPassword = settings.encryptionPassword,
                encryptionEncoding = settings.encryptionEncoding,
                onInteraction = onInteraction
            )
            NavigationBarSpacer()
        }
    }
}

@Composable
private fun SuggestionsCategory(
    suggestionTypes: ImmutableList<SuggestionType>,
    suggestionMode: SuggestionMode,
    onNavToCommands: () -> Unit,
    onInteraction: (ChatSettingsInteraction) -> Unit,
) {
    PreferenceCategory(title = stringResource(R.string.preference_suggestions_header)) {
        val suggestionEntries = listOf(
            stringResource(R.string.preference_suggestions_emotes),
            stringResource(R.string.preference_suggestions_users),
            stringResource(R.string.preference_suggestions_commands),
            stringResource(R.string.preference_suggestions_supibot),
        ).toImmutableList()
        val suggestionDescriptions = listOf(
            stringResource(R.string.preference_suggestions_emotes_desc),
            stringResource(R.string.preference_suggestions_users_desc),
            stringResource(R.string.preference_suggestions_commands_desc),
            stringResource(R.string.preference_suggestions_supibot_desc),
        ).toImmutableList()
        PreferenceMultiListDialog(
            title = stringResource(R.string.preference_suggestions_title),
            summary = stringResource(R.string.preference_suggestions_summary),
            values = remember { SuggestionType.entries.toImmutableList() },
            initialSelected = suggestionTypes,
            entries = suggestionEntries,
            descriptions = suggestionDescriptions,
            onChange = { onInteraction(ChatSettingsInteraction.SuggestionTypes(it)) },
        )
        val modeAutomatic = stringResource(R.string.preference_suggestion_mode_automatic)
        val modePrefixOnly = stringResource(R.string.preference_suggestion_mode_prefix_only)
        val modeEntries = remember { listOf(modeAutomatic, modePrefixOnly).toImmutableList() }
        PreferenceListDialog(
            title = stringResource(R.string.preference_suggestion_mode_title),
            summary = modeEntries[suggestionMode.ordinal],
            values = SuggestionMode.entries.toImmutableList(),
            entries = modeEntries,
            selected = suggestionMode,
            onChange = { onInteraction(ChatSettingsInteraction.SuggestionModeChange(it)) },
        )
        PreferenceItem(
            title = stringResource(R.string.commands_title),
            onClick = onNavToCommands,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
        )
    }
}

@Composable
private fun MessagesCategory(
    scrollbackLength: Int,
    showTimedOutMessages: Boolean,
    showTimestamps: Boolean,
    timestampFormat: String,
    onInteraction: (ChatSettingsInteraction) -> Unit,
) {
    PreferenceCategory(title = stringResource(R.string.preference_messages_header)) {
        var sliderValue by remember(scrollbackLength) { mutableFloatStateOf(scrollbackLength.toFloat()) }
        SliderPreferenceItem(
            title = stringResource(R.string.preference_scrollback_length_title),
            value = sliderValue,
            range = 50f..1000f,
            steps = 18,
            onDrag = { sliderValue = it },
            onDragFinish = { onInteraction(ChatSettingsInteraction.ScrollbackLength(sliderValue.roundToInt())) },
            displayValue = false,
            summary = sliderValue.roundToInt().toString(),
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_show_timed_out_messages_title),
            isChecked = showTimedOutMessages,
            onClick = { onInteraction(ChatSettingsInteraction.ShowTimedOutMessages(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_timestamp_title),
            isChecked = showTimestamps,
            onClick = { onInteraction(ChatSettingsInteraction.ShowTimestamps(it)) },
        )
        val timestampFormats = stringArrayResource(R.array.timestamp_formats).toImmutableList()
        PreferenceListDialog(
            title = stringResource(R.string.preference_timestamp_format_title),
            summary = timestampFormat,
            values = timestampFormats,
            entries = timestampFormats,
            selected = timestampFormat,
            onChange = { onInteraction(ChatSettingsInteraction.TimestampFormat(it)) },
        )
    }
}

@Composable
private fun UsersCategory(
    showUsernames: Boolean,
    userLongClickBehavior: UserLongClickBehavior,
    colorizeNicknames: Boolean,
    onNavToUserDisplays: () -> Unit,
    onInteraction: (ChatSettingsInteraction) -> Unit,
) {
    PreferenceCategory(title = stringResource(R.string.preference_users_header)) {
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_show_username_title),
            isChecked = showUsernames,
            onClick = { onInteraction(ChatSettingsInteraction.ShowUsernames(it)) },
        )
        val longClickSummaryOn = stringResource(R.string.preference_user_long_click_summary_on)
        val longClickSummaryOff = stringResource(R.string.preference_user_long_click_summary_off)
        val longClickEntries = remember { listOf(longClickSummaryOn, longClickSummaryOff).toImmutableList() }
        PreferenceListDialog(
            title = stringResource(R.string.preference_user_long_click_title),
            summary = longClickEntries[userLongClickBehavior.ordinal],
            values = UserLongClickBehavior.entries.toImmutableList(),
            entries = longClickEntries,
            selected = userLongClickBehavior,
            onChange = { onInteraction(ChatSettingsInteraction.UserLongClick(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_colorize_nicknames_title),
            summary = stringResource(R.string.preference_colorize_nicknames_summary),
            isChecked = colorizeNicknames,
            onClick = { onInteraction(ChatSettingsInteraction.ColorizeNicknames(it)) },
        )
        PreferenceItem(
            title = stringResource(R.string.custom_user_display_title),
            summary = stringResource(R.string.custom_user_display_summary),
            onClick = onNavToUserDisplays,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
        )
    }
}

@Composable
private fun EmotesAndBadgesCategory(
    animateGifs: Boolean,
    visibleBadges: ImmutableList<VisibleBadges>,
    visibleEmotes: ImmutableList<VisibleThirdPartyEmotes>,
    onInteraction: (ChatSettingsInteraction) -> Unit,
) {
    PreferenceCategory(title = stringResource(R.string.preference_emotes_badges_header)) {
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_animate_gifs_title),
            isChecked = animateGifs,
            onClick = { onInteraction(ChatSettingsInteraction.AnimateGifs(it)) },
        )
        val badgeEntries =
            stringArrayResource(R.array.badges_entries)
                .plus(stringResource(R.string.shared_chat))
                .toImmutableList()
        PreferenceMultiListDialog(
            title = stringResource(R.string.preference_visible_badges_title),
            initialSelected = visibleBadges,
            values = VisibleBadges.entries.toImmutableList(),
            entries = badgeEntries,
            onChange = { onInteraction(ChatSettingsInteraction.Badges(it)) },
        )
        PreferenceMultiListDialog(
            title = stringResource(R.string.preference_visible_emotes_title),
            initialSelected = visibleEmotes,
            values = VisibleThirdPartyEmotes.entries.toImmutableList(),
            entries = stringArrayResource(R.array.emotes_entries).toImmutableList(),
            onChange = { onInteraction(ChatSettingsInteraction.Emotes(it)) },
        )
    }
}

@Composable
private fun TinyCategory(onInteraction: (ChatSettingsInteraction) -> Unit, onNavToInstances: () -> Unit) {
    PreferenceCategory(title = stringResource(R.string.preference_tiny_category_title)) {
        PreferenceItem(
            title = stringResource(R.string.tiny_instances),
            summary = stringResource(R.string.tiny_instances_summary),
            onClick = onNavToInstances,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
        )
    }
}

@Composable
private fun EncryptionCategory(
    enableMessageEncryption: Boolean,
    randomSpaces: Boolean,
    encryptionEncoding: ChatEncryption.Encoding,
    encryptionPassword: String,
    onInteraction: (ChatSettingsInteraction) -> Unit
) {
    PreferenceCategory(title = stringResource(R.string.preference_encryption_category_title)) {
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_enable_message_encryption),
            summary = stringResource(R.string.preference_enable_message_encryption_summary),
            isChecked = enableMessageEncryption,
            onClick = { onInteraction(ChatSettingsInteraction.EnableMessageEncryption(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_add_random_spaces),
            summary = stringResource(R.string.preference_add_random_spaces_summary),
            isChecked = randomSpaces,
            onClick = { onInteraction(ChatSettingsInteraction.RandomEncryptedSpaces(it)) },
        )
        PreferenceListDialog(
            title = stringResource(R.string.preference_encryption_encoding),
            summary = encryptionEncoding.name,
            values = ChatEncryption.Encoding.entries.toImmutableList(),
            entries = ChatEncryption.Encoding.entries.map { it.name }.toImmutableList(),
            selected = encryptionEncoding,
            onChange = { onInteraction(ChatSettingsInteraction.EncryptionEncoding(it)) },
        )
        ExpandablePreferenceItem (title = stringResource(R.string.preference_encryption_password)) {
            EncryptionPasswordBottomSheet(
                initialPassword = encryptionPassword,
                onInteraction = {
                    dismiss()
                    onInteraction(it)
                },
            )
        }
    }
}

@Composable
private fun SevenTVCategory(
    enabled: Boolean,
    allowUnlistedSevenTvEmotes: Boolean,
    sevenTVLiveEmoteUpdates: Boolean,
    onInteraction: (ChatSettingsInteraction) -> Unit,
    onNavToBattery: () -> Unit,
) {
    PreferenceCategory(title = stringResource(R.string.preference_7tv_category_title)) {
        SwitchPreferenceItem(
            isEnabled = enabled,
            title = stringResource(R.string.preference_unlisted_emotes_title),
            summary = stringResource(R.string.preference_unlisted_emotes_summary),
            isChecked = allowUnlistedSevenTvEmotes,
            onClick = { onInteraction(ChatSettingsInteraction.AllowUnlisted(it)) },
        )
        SwitchPreferenceItem(
            isEnabled = enabled,
            title = stringResource(R.string.preference_7tv_live_updates_title),
            isChecked = sevenTVLiveEmoteUpdates,
            onClick = { onInteraction(ChatSettingsInteraction.LiveEmoteUpdates(it)) },
        )
        PreferenceItem(
            title = stringResource(R.string.preference_battery_pause_7tv_title),
            onClick = onNavToBattery,
        )
    }
}

@Composable
private fun MessageHistoryCategory(
    loadMessageHistory: Boolean,
    loadMessageHistoryAfterReconnect: Boolean,
    messageHistoryDashboardUrl: String,
    onInteraction: (ChatSettingsInteraction) -> Unit,
) {
    val launcher = LocalUriHandler.current
    PreferenceCategory(title = stringResource(R.string.preference_message_history_header)) {
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_load_message_history_title),
            isChecked = loadMessageHistory,
            onClick = { onInteraction(ChatSettingsInteraction.MessageHistory(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_load_messages_on_reconnect_title),
            summary = stringResource(R.string.preference_load_messages_on_reconnect_summary),
            isChecked = loadMessageHistoryAfterReconnect,
            onClick = { onInteraction(ChatSettingsInteraction.MessageHistoryAfterReconnect(it)) },
        )
        PreferenceItem(
            title = stringResource(R.string.preference_message_history_dashboard_title),
            summary = stringResource(R.string.preference_message_history_dashboard_summary),
            onClick = { launcher.openUri(messageHistoryDashboardUrl) },
        )
    }
}

@Composable
private fun ChannelDataCategory(
    showChatModes: Boolean,
    alwaysShowPinnedMessage: Boolean,
    showStreamTitleInLiveMessage: Boolean,
    onInteraction: (ChatSettingsInteraction) -> Unit,
) {
    PreferenceCategory(title = stringResource(R.string.preference_channel_data_header)) {
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_roomstate_title),
            summary = stringResource(R.string.preference_roomstate_summary),
            isChecked = showChatModes,
            onClick = { onInteraction(ChatSettingsInteraction.ChatModes(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_always_show_pinned_message_title),
            summary = stringResource(R.string.preference_always_show_pinned_message_summary),
            isChecked = alwaysShowPinnedMessage,
            onClick = { onInteraction(ChatSettingsInteraction.AlwaysShowPinnedMessage(it)) },
        )
        SwitchPreferenceItem(
            title = stringResource(R.string.preference_show_stream_title_in_live_message_title),
            isChecked = showStreamTitleInLiveMessage,
            onClick = { onInteraction(ChatSettingsInteraction.ShowStreamTitleInLiveMessage(it)) },
        )
    }
}


@Composable
private fun EncryptionPasswordBottomSheet(
    initialPassword: String,
    onInteraction: (ChatSettingsInteraction) -> Unit,
) {
    var password by remember(initialPassword) { mutableStateOf(initialPassword) }
    ModalBottomSheet(onDismissRequest = { onInteraction(ChatSettingsInteraction.EncryptionPassword(password)) }) {
        Text(
            text = stringResource(R.string.preference_encryption_password),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Dp.Hairline, end = Dp.Hairline, bottom = Dp.Hairline),
        )
        TextButton(
            onClick = { password = "CHANGE_THIS" },
            content = { Text(stringResource(R.string.reset)) },
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = Dp.Hairline),
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dp.Hairline),
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.preference_encryption_password)) },
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
            ),
        )
        Spacer(Modifier.height(Dp.Hairline))
    }
}
