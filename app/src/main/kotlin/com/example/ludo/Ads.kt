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
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
 * once consent allows it. Banners sit below the content; interstitials only at screen transitions.
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
        thread { MobileAds.initialize(activity) { activity.runOnUiThread { _ready.value = true; loadInterstitial() } } } // the SDK asks for a background thread
    }

    private var interstitial: InterstitialAd? = null
    private var lastShown = 0L

    private fun loadInterstitial() {
        InterstitialAd.load(activity, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null }
        })
    }

    /** Shows an interstitial if one is loaded and the cooldown has passed, then runs [then]; otherwise runs [then] at once. */
    fun showInterstitial(then: () -> Unit) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (ad == null || now - lastShown < COOLDOWN_MS) return then()
        interstitial = null
        lastShown = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadInterstitial(); then() }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { loadInterstitial(); then() }
        }
        ad.show(activity)
    }

    private companion object { const val COOLDOWN_MS = 120_000L } // ponytail: one fixed cap; tune from AdMob stats if needed
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
