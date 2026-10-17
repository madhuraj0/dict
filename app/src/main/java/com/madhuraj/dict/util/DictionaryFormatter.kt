package com.madhuraj.dict.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.madhuraj.dict.data.model.WordSense

object DictionaryFormatter {

    fun isDarkTheme(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView(
        webView: WebView,
        onWordClick: (String) -> Unit
    ) {
        webView.setBackgroundColor(0)
        webView.settings.apply {
            javaScriptEnabled = false
            loadWithOverviewMode = true
            useWideViewPort = false
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {
            private fun handleUrlClick(url: String?): Boolean {
                if (url == null) return false
                val word = when {
                    url.startsWith("wi:") -> url.substringAfter("wi:")
                    url.startsWith("dict:") -> url.substringAfter("dict:")
                    url.contains("/wiki/") -> url.substringAfter("/wiki/")
                    else -> return false
                }.substringBefore("#").trim()

                if (word.isNotEmpty() && !word.equals("IPA", ignoreCase = true)) {
                    onWordClick(word)
                    return true
                }
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return handleUrlClick(request?.url?.toString())
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUrlClick(url)
            }
        }
    }

    fun buildDictionaryPage(
        bodyHtml: String,
        isDark: Boolean
    ): String {
        val bgHex = if (isDark) "#121212" else "#FFFFFF"
        val textHex = if (isDark) "#E2E8F0" else "#1E293B"
        val headHex = if (isDark) "#F8FAFC" else "#0F172A"
        val linkHex = if (isDark) "#FFD54F" else "#B45309"
        val dividerHex = if (isDark) "#2D3748" else "#E2E8F0"
        val subHex = if (isDark) "#94A3B8" else "#64748B"

        // Strip silence tags or unnecessary internal Livio markers
        val cleaned = bodyHtml
            .replace("<silence>", "")
            .replace("</silence>", "")
            .replace("<hr style='clear:both'>", "<hr>")
            .replace("<hr style=\"clear:both\">", "<hr>")

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; }
                body {
                    background-color: $bgHex;
                    color: $textHex;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    font-size: 16px;
                    line-height: 1.55;
                    margin: 0;
                    padding: 0 4px 32px 4px;
                    -webkit-tap-highlight-color: transparent;
                }
                .head {
                    display: block;
                    font-size: 20px;
                    font-weight: 700;
                    font-style: italic;
                    color: $headHex;
                    margin-top: 18px;
                    margin-bottom: 8px;
                    letter-spacing: -0.2px;
                }
                hr {
                    border: none;
                    border-top: 1px solid $dividerHex;
                    margin: 16px 0;
                }
                a {
                    color: $linkHex;
                    text-decoration: none;
                    font-weight: 600;
                }
                a:active {
                    opacity: 0.65;
                }
                ol {
                    margin: 8px 0;
                    padding-left: 22px;
                }
                ol > li {
                    margin-bottom: 12px;
                }
                ul {
                    margin: 8px 0;
                    padding-left: 20px;
                }
                ul > li {
                    margin-bottom: 6px;
                }
                dl {
                    margin: 8px 0;
                    padding-left: 8px;
                }
                dd {
                    margin-left: 12px;
                    color: $subHex;
                    font-size: 14.5px;
                }
                p {
                    margin: 8px 0;
                }
                i, em {
                    font-style: italic;
                    color: $textHex;
                }
                b, strong {
                    font-weight: 700;
                    color: $headHex;
                }
                small {
                    font-size: 13px;
                    color: $subHex;
                }
            </style>
            </head>
            <body>
            $cleaned
            </body>
            </html>
        """.trimIndent()
    }

    fun formatSensesToHtml(
        word: String,
        ipa: String?,
        senses: List<WordSense>,
        isDark: Boolean
    ): String {
        val sb = StringBuilder()

        if (!ipa.isNullOrBlank()) {
            sb.append("<span class=head>Pronunciation</span>")
            sb.append("<ul><li><small>IPA</small>: $ipa</li></ul>")
            sb.append("<hr>")
        }

        sb.append("<p><b>${htmlEscape(word)}</b></p>")

        val grouped = senses.groupBy { it.pos?.lowercase() ?: "general" }
        for ((pos, senseList) in grouped) {
            val title = pos.replaceFirstChar { it.uppercase() }
            sb.append("<span class=head>$title</span>")
            sb.append("<ol>")
            for (sense in senseList) {
                sb.append("<li>")
                sb.append(htmlEscape(sense.definition))

                if (sense.examples.isNotEmpty()) {
                    sb.append("<dl>")
                    for (ex in sense.examples) {
                        sb.append("<dd><i>“${htmlEscape(ex)}”</i></dd>")
                    }
                    sb.append("</dl>")
                }

                if (sense.synonyms.isNotEmpty()) {
                    sb.append("<dl><dd><b>Synonyms: </b>")
                    val synLinks = sense.synonyms.joinToString(", ") { syn ->
                        "<a href=\"wi:${htmlEscape(syn)}\">${htmlEscape(syn)}</a>"
                    }
                    sb.append(synLinks)
                    sb.append("</dd></dl>")
                }
                sb.append("</li>")
            }
            sb.append("</ol>")
            sb.append("<hr>")
        }

        return buildDictionaryPage(sb.toString(), isDark)
    }

    private fun htmlEscape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
