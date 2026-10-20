package com.amoled.chromebrowser

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object PrivacyEngine {

    var adBlockEnabled = true
    var trackerBlockEnabled = true
    var antiFingerprintEnabled = true
    var blockThirdPartyCookies = true
    var autoClearCookiesOnExit = false
    var blockedCount = 0

    // Built-in Extensions State
    var extAmoledDarkPages = false
    var extYoutubeAdSkipper = true
    var extCleanTrackingUrls = true
    var customUserScripts = mutableListOf<String>()

    private val adAndTrackerDomains = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "connect.facebook.net",
        "analytics.tiktok.com",
        "adnxs.com",
        "criteo.com",
        "taboola.com",
        "outbrain.com",
        "scorecardresearch.com",
        "zedo.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "adsterra.com",
        "amazon-adsystem.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "smartadserver.com",
        "advertising.com",
        "quantserve.com",
        "hotjar.com",
        "mouseflow.com",
        "clarity.ms",
        "sentry.io"
    )

    fun shouldBlockRequest(url: String): Boolean {
        if (!adBlockEnabled && !trackerBlockEnabled) return false
        val lower = url.lowercase()
        for (domain in adAndTrackerDomains) {
            if (lower.contains(domain)) {
                blockedCount++
                return true
            }
        }
        return false
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun buildInjectedJavaScript(): String {
        val sb = StringBuilder()

        // 1. Anti-Fingerprinting & Privacy Protection
        if (antiFingerprintEnabled) {
            sb.append("""
                (function() {
                    try {
                        Object.defineProperty(navigator, 'doNotTrack', { get: () => '1' });
                        Object.defineProperty(navigator, 'globalPrivacyControl', { get: () => true });
                        navigator.sendBeacon = function() { return true; };
                    } catch(e) {}
                })();
            """.trimIndent())
        }

        // 2. Cosmetic Ad Filtering & YouTube Ad Skipper Extension
        if (extYoutubeAdSkipper) {
            sb.append("""
                (function() {
                    const style = document.createElement('style');
                    style.innerHTML = `
                        .ad-showing, .ytp-ad-overlay-container, .ytp-ad-message-container,
                        ytm-promoted-sparkles-web-renderer, ytm-companion-ad-renderer,
                        .adsbygoogle, [id^="google_ads_"], [class*="ad-banner"], [class*="sponsored"] {
                            display: none !important;
                        }
                    `;
                    document.documentElement.appendChild(style);
                    setInterval(() => {
                        const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                        if (skipBtn) skipBtn.click();
                        const video = document.querySelector('video');
                        if (document.querySelector('.ad-showing') && video && !isNaN(video.duration)) {
                            video.currentTime = video.duration;
                        }
                    }, 600);
                })();
            """.trimIndent())
        }

        // 3. Pure AMOLED Dark Mode Extension for All Websites
        if (extAmoledDarkPages) {
            sb.append("""
                (function() {
                    const amoledStyle = document.createElement('style');
                    amoledStyle.innerHTML = `
                        html, body, main, article, section, nav, header, footer {
                            background-color: #000000 !important;
                            color: #E8EAED !important;
                        }
                        div, p, span, li, td, th {
                            border-color: #222222 !important;
                        }
                        a { color: #8AB4F8 !important; }
                        input, textarea, select, button {
                            background-color: #111111 !important;
                            color: #FFFFFF !important;
                            border: 1px solid #2A2A2A !important;
                        }
                    `;
                    document.documentElement.appendChild(amoledStyle);
                })();
            """.trimIndent())
        }

        // 4. Custom User Extensions
        for (script in customUserScripts) {
            sb.append("\n;try { ").append(script).append(" } catch(e) {}\n")
        }

        return sb.toString()
    }
}
