package com.team.taskmanagementapp.ui.base

/**
 * Sealed class representing UI States across the application.
 * Covers Loading, Success (with data payload), Error (with message), and Empty states.
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
