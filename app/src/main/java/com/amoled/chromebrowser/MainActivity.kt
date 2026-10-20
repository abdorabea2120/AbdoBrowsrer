package com.amoled.chromebrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webViewContainer: FrameLayout
    private lateinit var urlInput: EditText
    private lateinit var btnShield: TextView
    private lateinit var btnReload: TextView
    private lateinit var btnExtensions: TextView
    private lateinit var btnTabs: TextView
    private lateinit var btnHome: ImageButton
    private lateinit var progressBar: ProgressBar

    private val tabs = mutableListOf<WebView>()
    private var currentTabIndex = 0

    // Standard Chrome Mobile User-Agent so Google Sign-In & Google Services work 100% seamlessly
    private val chromeUserAgent =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36"

    private val amoledGoogleStartPage = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Google AMOLED</title>
        <style>
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: sans-serif; }
            body {
                background-color: #000000;
                color: #FFFFFF;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                height: 92vh;
                padding: 20px;
            }
            .logo {
                font-size: 52px;
                font-weight: 700;
                letter-spacing: -1.5px;
                margin-bottom: 28px;
                color: #FFFFFF;
            }
            .logo span.b { color: #8AB4F8; }
            .logo span.g { color: #34A853; }
            .search-bar {
                width: 100%;
                max-width: 520px;
                background: #0D0D0D;
                border: 1px solid #262626;
                border-radius: 28px;
                padding: 14px 20px;
                display: flex;
                align-items: center;
            }
            .search-bar input {
                flex: 1;
                background: transparent;
                border: none;
                color: #FFFFFF;
                font-size: 16px;
                outline: none;
            }
            .shortcuts {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 18px;
                margin-top: 36px;
                width: 100%;
                max-width: 420px;
                text-align: center;
            }
            .shortcut {
                background: #0B0B0B;
                border: 1px solid #1C1C1C;
                border-radius: 16px;
                padding: 14px 6px;
                color: #CCCCCC;
                text-decoration: none;
                font-size: 12px;
            }
            .badge {
                margin-top: 40px;
                font-size: 12px;
                color: #34A853;
                background: #071A0C;
                border: 1px solid #154221;
                padding: 8px 16px;
                border-radius: 20px;
            }
        </style>
        </head>
        <body>
            <div class="logo">G<span>o</span><span class="b">o</span><span class="g">g</span>le</div>
            <form class="search-bar" onsubmit="event.preventDefault(); window.location.href='https://www.google.com/search?q=' + encodeURIComponent(document.getElementById('q').value);">
                <input id="q" type="text" placeholder="ابحث في Google بأمان وخصوصية...">
            </form>
            <div class="shortcuts">
                <a class="shortcut" href="https://www.google.com">🔍<br>Google</a>
                <a class="shortcut" href="https://m.youtube.com">▶️<br>YouTube</a>
                <a class="shortcut" href="https://mail.google.com">✉️<br>Gmail</a>
                <a class="shortcut" href="https://drive.google.com">📁<br>Drive</a>
            </div>
            <div class="badge">🛡️ وضع الخصوصية القصوى + شاشة AMOLED سوداء 100%</div>
        </body>
        </html>
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webViewContainer = findViewById(R.id.webViewContainer)
        urlInput = findViewById(R.id.urlInput)
        btnShield = findViewById(R.id.btnShield)
        btnReload = findViewById(R.id.btnReload)
        btnExtensions = findViewById(R.id.btnExtensions)
        btnTabs = findViewById(R.id.btnTabs)
        btnHome = findViewById(R.id.btnHome)
        progressBar = findViewById(R.id.progressBar)

        setupToolbarListeners()

        val initialUrl = intent?.dataString
        createNewTab(initialUrl)
    }

    private fun setupToolbarListeners() {
        urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                loadUserInput(urlInput.text.toString())
                true
            } else {
                false
            }
        }

        btnHome.setOnClickListener {
            loadAmoledHomePage(currentWebView())
        }

        btnReload.setOnClickListener {
            currentWebView().reload()
        }

        btnShield.setOnClickListener {
            showPrivacyShieldDialog()
        }

        btnExtensions.setOnClickListener {
            showExtensionsDialog()
        }

        btnTabs.setOnClickListener {
            showTabsDialog()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createNewTab(initialUrl: String? = null) {
        val webView = WebView(this)
        webView.setBackgroundColor(Color.BLACK)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.userAgentString = chromeUserAgent
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        applyCookiePolicy(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: return null
                if (PrivacyEngine.shouldBlockRequest(reqUrl)) {
                    runOnUiThread {
                        btnShield.text = "🛡️ ${PrivacyEngine.blockedCount}"
                    }
                    return PrivacyEngine.createEmptyResponse()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                if (PrivacyEngine.extCleanTrackingUrls) {
                    val cleaned = cleanTrackingParams(uri)
                    if (cleaned != uri.toString()) {
                        view?.loadUrl(cleaned)
                        return true
                    }
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (view == currentWebView()) {
                    progressBar.visibility = View.VISIBLE
                    if (url != null && !url.startsWith("data:")) {
                        urlInput.setText(url)
                    } else {
                        urlInput.setText("chrome://amoled-google")
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == currentWebView()) {
                    progressBar.visibility = View.GONE
                }
                val js = PrivacyEngine.buildInjectedJavaScript()
                if (js.isNotBlank()) {
                    view?.evaluateJavascript(js, null)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (view == currentWebView()) {
                    progressBar.progress = newProgress
                }
            }
        }

        tabs.add(webView)
        switchToTab(tabs.size - 1)

        if (!initialUrl.isNullOrBlank()) {
            loadUserInput(initialUrl)
        } else {
            loadAmoledHomePage(webView)
        }
    }

    private fun switchToTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        currentTabIndex = index
        webViewContainer.removeAllViews()
        val activeView = tabs[currentTabIndex]
        webViewContainer.addView(activeView)
        btnTabs.text = tabs.size.toString()
        val currentUrl = activeView.url ?: "chrome://amoled-google"
        urlInput.setText(if (currentUrl.startsWith("data:")) "chrome://amoled-google" else currentUrl)
    }

    private fun currentWebView(): WebView = tabs[currentTabIndex]

    private fun loadAmoledHomePage(webView: WebView) {
        webView.loadDataWithBaseURL(
            "https://www.google.com",
            amoledGoogleStartPage,
            "text/html",
            "UTF-8",
            null
        )
        urlInput.setText("chrome://amoled-google")
    }

    private fun loadUserInput(rawInput: String) {
        val query = rawInput.trim()
        if (query.isEmpty() || query == "chrome://amoled-google") {
            loadAmoledHomePage(currentWebView())
            return
        }
        val targetUrl = when {
            query.startsWith("http://") || query.startsWith("https://") -> query
            query.contains(".") && !query.contains(" ") -> "https://$query"
            else -> "https://www.google.com/search?q=${Uri.encode(query)}"
        }
        currentWebView().loadUrl(targetUrl)
    }

    private fun cleanTrackingParams(uri: Uri): String {
        if (uri.queryParameterNames.isEmpty()) return uri.toString()
        val blockedParams = setOf("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "fbclid", "gclid", "mc_cid")
        val builder = uri.buildUpon().clearQuery()
        for (name in uri.queryParameterNames) {
            if (!blockedParams.contains(name.lowercase())) {
                for (value in uri.getQueryParameters(name)) {
                    builder.appendQueryParameter(name, value)
                }
            }
        }
        return builder.build().toString()
    }

    private fun applyCookiePolicy(webView: WebView) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, !PrivacyEngine.blockThirdPartyCookies)
    }

    private fun showPrivacyShieldDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 24)
            setBackgroundColor(Color.BLACK)
        }

        fun addSwitch(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
            val sw = Switch(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 14f
                isChecked = initial
                setPadding(0, 20, 0, 20)
                setOnCheckedChangeListener { _, checked -> onChange(checked) }
            }
            layout.addView(sw)
        }

        addSwitch("حظر الإعلانات والنوافذ المنبثقة (AdBlock)", PrivacyEngine.adBlockEnabled) {
            PrivacyEngine.adBlockEnabled = it
        }
        addSwitch("منع متتبعات الطرف الثالث (Trackers)", PrivacyEngine.trackerBlockEnabled) {
            PrivacyEngine.trackerBlockEnabled = it
        }
        addSwitch("حظر كوكيز الطرف الثالث (3rd-Party Cookies)", PrivacyEngine.blockThirdPartyCookies) {
            PrivacyEngine.blockThirdPartyCookies = it
            tabs.forEach { wv -> applyCookiePolicy(wv) }
        }
        addSwitch("حماية ضد بصمة المتصفح (Anti-Fingerprinting)", PrivacyEngine.antiFingerprintEnabled) {
            PrivacyEngine.antiFingerprintEnabled = it
        }
        addSwitch("مسح الكوكيز تلقائياً عند إغلاق التطبيق", PrivacyEngine.autoClearCookiesOnExit) {
            PrivacyEngine.autoClearCookiesOnExit = it
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🛡️ درع الخصوصية والكوكيز (محظور: ${PrivacyEngine.blockedCount})")
            .setView(layout)
            .setPositiveButton("تطبيق وتحديث") { _, _ ->
                currentWebView().reload()
            }
            .setNeutralButton("مسح الكوكيز والكاش الآن") { _, _ ->
                clearAllBrowserData()
            }
            .show()
    }

    private fun showExtensionsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 24)
            setBackgroundColor(Color.BLACK)
        }

        fun addSwitch(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
            val sw = Switch(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 14f
                isChecked = initial
                setPadding(0, 20, 0, 20)
                setOnCheckedChangeListener { _, checked -> onChange(checked) }
            }
            layout.addView(sw)
        }

        addSwitch("🌙 Dark Reader AMOLED (تحويل المواقع للأسود النقي)", PrivacyEngine.extAmoledDarkPages) {
            PrivacyEngine.extAmoledDarkPages = it
        }
        addSwitch("▶️ YouTube AdSkipper (تخطي وحجب إعلانات يوتيوب)", PrivacyEngine.extYoutubeAdSkipper) {
            PrivacyEngine.extYoutubeAdSkipper = it
        }
        addSwitch("🔗 CleanURLs (حذف روابط التتبع utm و fbclid)", PrivacyEngine.extCleanTrackingUrls) {
            PrivacyEngine.extCleanTrackingUrls = it
        }

        val customInput = EditText(this).apply {
            hint = "أضف كود إضافة JavaScript / UserScript مخصص..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 13f
        }
        layout.addView(customInput)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🧩 مدير الإضافات (Extensions)")
            .setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton("حفظ وتفعيل") { _, _ ->
                val script = customInput.text.toString().trim()
                if (script.isNotEmpty()) {
                    PrivacyEngine.customUserScripts.add(script)
                    Toast.makeText(this, "تم تثبيت الإضافة بنجاح!", Toast.LENGTH_SHORT).show()
                }
                currentWebView().reload()
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun showTabsDialog() {
        val tabTitles = tabs.mapIndexed { idx, wv ->
            val title = wv.title?.takeIf { it.isNotBlank() } ?: "تبويب ${idx + 1}"
            if (idx == currentTabIndex) "● $title" else "   $title"
        }.toTypedArray()

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("التبويبات المفتوحة (${tabs.size})")
            .setItems(tabTitles) { _, which ->
                switchToTab(which)
            }
            .setPositiveButton("+ تبويب جديد") { _, _ ->
                createNewTab()
            }
            .setNeutralButton("إغلاق التبويب الحالي") { _, _ ->
                if (tabs.size > 1) {
                    val removed = tabs.removeAt(currentTabIndex)
                    removed.destroy()
                    switchToTab(0)
                } else {
                    loadAmoledHomePage(currentWebView())
                }
            }
            .show()
    }

    private fun clearAllBrowserData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        tabs.forEach {
            it.clearCache(true)
            it.clearFormData()
            it.clearHistory()
        }
        Toast.makeText(this, "تم مسح جميع الكوكيز وبيانات التصفح!", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (currentWebView().canGoBack()) {
            currentWebView().goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (PrivacyEngine.autoClearCookiesOnExit) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        super.onDestroy()
    }
}
