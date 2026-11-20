package com.example.data.adapters

import android.content.Context
import com.example.data.model.AnalyzedMediaResult
import com.example.data.model.PlatformType

interface MediaSourceAdapter {
    val platform: PlatformType
    fun matches(urlOrUri: String, mimeType: String? = null): Boolean
    suspend fun analyze(context: Context, urlOrUri: String, mimeType: String? = null): AnalyzedMediaResult
}
