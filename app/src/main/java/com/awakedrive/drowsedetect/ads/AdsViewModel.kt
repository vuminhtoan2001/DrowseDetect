package com.awakedrive.drowsedetect.ads

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val ADS_CACHE_TAG = "AdsCache"

/**
 * Activity-scoped cache for native ads. Each [Slot] keeps the loaded NativeAd and
 * its load state alive across recompositions / list recycling / navigation, so a
 * native ad does not reload every time its composable re-enters composition.
 *
 * Logic mirrors the example; only the loader/renderer (NativeAdSlot) was swapped
 * to use the com.nlbn.ads library instead of GMS AdLoader directly.
 */
class AdsViewModel : ViewModel() {
    private val slots = mutableMapOf<String, Slot>()

    fun slot(name: String): Slot = slots.getOrPut(name) { Slot(name) }

    override fun onCleared() {
        slots.values.forEach { it.destroy() }
        slots.clear()
    }

    class Slot(private val name: String = "") {
        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _isLoaded = MutableStateFlow(false)
        val isLoaded: StateFlow<Boolean> = _isLoaded

        private val _nativeAd = MutableStateFlow<NativeAd?>(null)
        val nativeAd: StateFlow<NativeAd?> = _nativeAd

        private val _impressionRecorded = MutableStateFlow(false)
        val impressionRecorded: StateFlow<Boolean> = _impressionRecorded

        private var lastRequestAtMs: Long = 0L
        private var lastRequestFailed: Boolean = false

        fun onStartLoad() {
            Log.d(ADS_CACHE_TAG, "[$name] REQUEST NEW AD (t=${System.currentTimeMillis()})")
            _isLoading.value = true
            _isLoaded.value = false
            lastRequestAtMs = System.currentTimeMillis()
            lastRequestFailed = false
        }

        fun onLoaded() {
            Log.d(ADS_CACHE_TAG, "[$name] load result: SUCCESS (cached, no impression yet)")
            _isLoading.value = false
            _isLoaded.value = true
            lastRequestFailed = false
        }

        fun onLoadedNative(ad: NativeAd) {
            _nativeAd.value?.let { if (it !== ad) try { it.destroy() } catch (_: Exception) {} }
            _nativeAd.value = ad
        }

        fun onError() {
            Log.d(ADS_CACHE_TAG, "[$name] load result: FAILED")
            _isLoading.value = false
            _isLoaded.value = false
            _nativeAd.value = null
            lastRequestFailed = true
        }

        fun onImpression() {
            Log.d(ADS_CACHE_TAG, "[$name] IMPRESSION recorded")
            _impressionRecorded.value = true
        }

        /** Called when the slot's composable enters composition. Invalidates the
         *  cache (forcing a fresh load) once [minIntervalMs] has elapsed since the
         *  last request; otherwise keeps the cached ad. */
        fun prepareForEntry(minIntervalMs: Long = 0L) {
            val hadImpression = _impressionRecorded.value
            if (hadImpression) {
                _isLoading.value = false
                _impressionRecorded.value = false
            }
            val elapsed = System.currentTimeMillis() - lastRequestAtMs
            val timeAvailable = elapsed >= minIntervalMs
            Log.d(
                ADS_CACHE_TAG,
                "[$name] ENTER screen -> elapsed=${elapsed}ms / minInterval=${minIntervalMs}ms, " +
                    "isLoaded=${_isLoaded.value}, impressionRecordedBefore=$hadImpression " +
                    "=> ${if (timeAvailable) "INVALIDATE (will request new)" else "KEEP CACHE (no new request)"}"
            )
            if (timeAvailable) {
                _isLoaded.value = false
                if (_nativeAd.value != null) {
                    try { _nativeAd.value?.destroy() } catch (_: Exception) {}
                    _nativeAd.value = null
                }
            }
        }

        fun shouldLoadOnMount(): Boolean {
            return !(_isLoading.value || _isLoaded.value || _nativeAd.value != null)
        }

        fun destroy() {
            try { _nativeAd.value?.destroy() } catch (_: Exception) {}
            _nativeAd.value = null
            _isLoaded.value = false
            _isLoading.value = false
        }
    }
}
