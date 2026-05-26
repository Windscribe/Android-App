package com.windscribe.vpn.repository
sealed interface RepositoryEvent {
    data object Refresh : RepositoryEvent
    data object ForceRefresh : RepositoryEvent
}