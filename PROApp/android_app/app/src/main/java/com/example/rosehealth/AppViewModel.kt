package com.example.rosehealth

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared ViewModel that holds the scan state (bitmap + results) so
 * both ScanScreen and DiagnosisScreen can read from the same source.
 */
class AppViewModel : ViewModel() {

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val _results = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val results: StateFlow<List<Pair<String, Float>>> = _results.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setBitmap(bmp: Bitmap?) {
        _bitmap.value = bmp
        _results.value = emptyList() // reset results when image changes
    }

    fun setResults(r: List<Pair<String, Float>>) {
        _results.value = r
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
    }
}
