package com.kaijen.btpower.ui.sessiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaijen.btpower.data.db.entities.SampleEntity
import com.kaijen.btpower.data.db.entities.SessionEntity
import com.kaijen.btpower.data.repositories.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    val state: StateFlow<SessionDetailUiState> = combine(
        sessionRepository.observeSession(sessionId),
        sessionRepository.observeSamples(sessionId),
    ) { session, samples ->
        SessionDetailUiState(session = session, samples = samples)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), SessionDetailUiState())

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

data class SessionDetailUiState(
    val session: SessionEntity? = null,
    val samples: List<SampleEntity> = emptyList(),
)
