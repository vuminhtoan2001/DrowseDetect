package com.camera.qrscanner.barcode.ads

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import java.net.URLDecoder
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Xác định cài đặt từ QUẢNG CÁO hay ORGANIC qua Google Play Install Referrer
 * (port AppLifecycleModule.isAdsCampaign của RN).
 *
 * Quy tắc: có gclid → ads; utm_medium rỗng/"(not set)"/"organic" → organic; còn lại → ads.
 * Lỗi/không kết nối được → trả true (mặc định như RN).
 */
object CampaignHelper {

    private const val TAG = "CampaignHelper"

    suspend fun resolveIsAdsCampaign(context: Context): Boolean =
        suspendCancellableCoroutine { cont ->
            val done = java.util.concurrent.atomic.AtomicBoolean(false)
            fun finish(value: Boolean) {
                if (done.compareAndSet(false, true) && cont.isActive) cont.resume(value)
            }
            try {
                val client = InstallReferrerClient.newBuilder(context).build()
                client.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        try {
                            if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                                val rawRef = try {
                                    client.installReferrer.installReferrer ?: ""
                                } catch (e: Exception) {
                                    Log.e(TAG, "get installReferrer failed", e); ""
                                }
                                val decoded = try {
                                    URLDecoder.decode(rawRef, "UTF-8")
                                } catch (e: Exception) {
                                    rawRef
                                }
                                val uri = try {
                                    Uri.parse("https://dummy?$decoded")
                                } catch (e: Exception) {
                                    Uri.EMPTY
                                }
                                val utmMedium = uri.getQueryParameter("utm_medium")?.lowercase()
                                val gclid = uri.getQueryParameter("gclid")
                                val isAds = when {
                                    !gclid.isNullOrEmpty() -> true
                                    utmMedium.isNullOrEmpty() || utmMedium == "(not set)" -> false
                                    utmMedium == "organic" -> false
                                    else -> true
                                }
                                finish(isAds)
                            } else {
                                finish(true) // non-OK → mặc định như RN
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "isAdsCampaign error", e); finish(true)
                        } finally {
                            try { client.endConnection() } catch (_: Exception) {}
                        }
                    }

                    override fun onInstallReferrerServiceDisconnected() { finish(true) }
                })
                cont.invokeOnCancellation { try { client.endConnection() } catch (_: Exception) {} }
            } catch (e: Exception) {
                Log.e(TAG, "startConnection failed", e); finish(true)
            }
        }
}
