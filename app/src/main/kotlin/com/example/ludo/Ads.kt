package com.example.ludo

import android.app.Activity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.thread

/**
 * AdMob behind Google's consent form (UMP): the SDK is only initialised, and ads only requested,
 * once consent allows it. Banners appear on menu screens only, never during a game.
 */
class Ads(private val activity: Activity) {
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private var initialising = false

    private val _ready = MutableStateFlow(false)
    /** True once the SDK is initialised and ads may be requested. */
    val ready: StateFlow<Boolean> = _ready

    private val _privacyOptions = MutableStateFlow(false)
    /** True when the user's region requires an "Ad privacy settings" entry point. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptions

    fun start() {
        consent.requestConsentInfoUpdate(
            activity, ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refresh() } },
            { refresh() },
        )
        refresh() // consent from an earlier session may already allow ads
    }

    fun showPrivacyOptions() = UserMessagingPlatform.showPrivacyOptionsForm(activity) { refresh() }

    private fun refresh() {
        _privacyOptions.value = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds() || initialising) return
        initialising = true
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG).build())
        thread { MobileAds.initialize(activity) { _ready.value = true } } // the SDK asks for a background thread
    }
}

/** Anchored adaptive banner; draws nothing until ads are allowed. */
@Composable
fun AdBanner(ads: Ads, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // ponytail: deprecated but still the standard-height anchored banner; the "large" replacement is much taller
        @Suppress("DEPRECATION")
        val size = remember(maxWidth) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, maxWidth.value.toInt()) }
        val view = remember(size) {
            AdView(context).apply { setAdSize(size); adUnitId = BuildConfig.ADMOB_BANNER_ID; loadAd(AdRequest.Builder().build()) }
        }
        DisposableEffect(view) { onDispose { view.destroy() } }
        AndroidView({ view }, Modifier.fillMaxWidth().height(size.height.dp))
    }
}
