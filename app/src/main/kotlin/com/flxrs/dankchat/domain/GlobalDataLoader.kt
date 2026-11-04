package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class GlobalDataLoader(
    private val dataRepository: DataRepository,
    private val commandRepository: CommandRepository,
    private val dispatchersProvider: DispatchersProvider,
    private val chatSettingsDataStore: ChatSettingsDataStore
) {
    suspend fun loadGlobalData(): List<Result<Unit>> = withContext(dispatchersProvider.io) {
        val defers = mutableListOf<Deferred<Result<Unit>>>()
        defers.add(async { loadDankChatBadges() })
        defers.add(async { loadGlobalBTTVEmotes() })
        defers.add(async { loadGlobalFFZEmotes() })
        defers.add(async { loadGlobalSevenTVEmotes() })
        chatSettingsDataStore.tinyEmoteInstances.first().forEach { if (it.globalEmotes) {
            defers.add(async { loadGlobalTinyEmotes(it.url) })
        } }
        val results = awaitAll(*defers.toTypedArray())
        // Best-effort load, must not delay the global loading state
        commandRepository.loadSupibotCommands()
        results
    }

    suspend fun loadAuthGlobalData(): List<Result<Unit>> = withContext(dispatchersProvider.io) {
        awaitAll(
            async { loadGlobalBadges() },
        )
    }

    suspend fun loadDankChatBadges(): Result<Unit> = dataRepository.loadDankChatBadges()

    suspend fun loadGlobalBadges(): Result<Unit> = dataRepository.loadGlobalBadges()

    suspend fun loadGlobalBTTVEmotes(forceNetwork: Boolean = false): Result<Unit> = dataRepository.loadGlobalBTTVEmotes(forceNetwork)

    suspend fun loadGlobalFFZEmotes(forceNetwork: Boolean = false): Result<Unit> = dataRepository.loadGlobalFFZEmotes(forceNetwork)

    suspend fun loadGlobalSevenTVEmotes(forceNetwork: Boolean = false): Result<Unit> = dataRepository.loadGlobalSevenTVEmotes(forceNetwork)

    suspend fun loadGlobalTinyEmotes(instanceUrl: String, forceNetwork: Boolean = false): Result<Unit> = dataRepository.loadGlobalTinyEmotes(instanceUrl, forceNetwork)

    suspend fun loadUserEmotes(
        userId: UserId,
        onFirstPageLoaded: (() -> Unit)? = null,
    ): Result<Unit> = dataRepository.loadUserEmotes(userId, onFirstPageLoaded)
}
