package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ImageRepository {
    private val _lastCapturedImage = MutableStateFlow<ByteArray?>(null)
    val lastCapturedImage: StateFlow<ByteArray?> = _lastCapturedImage

    fun setLastCapturedImage(imageBytes: ByteArray) {
        _lastCapturedImage.value = imageBytes
    }
}
