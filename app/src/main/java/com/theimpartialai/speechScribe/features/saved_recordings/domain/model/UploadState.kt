package com.theimpartialai.speechScribe.features.saved_recordings.domain.model

sealed class UploadState {
    data object Idle : UploadState()
    data object Loading : UploadState()
    data class Success(val message: String) : UploadState()
    data class Error(val message: String) : UploadState()
}