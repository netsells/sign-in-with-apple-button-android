package com.willowtreeapps.signinwithapplebutton.view

import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleResult
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleService
import com.willowtreeapps.signinwithapplebutton.view.SignInWithAppleButton.Companion.SIGN_IN_WITH_APPLE_LOG_TAG

internal class SignInWebViewClient(
    private val attempt: SignInWithAppleService.AuthenticationAttempt,
    private val callback: (SignInWithAppleResult) -> Unit,
    private val javascriptCallback: () -> Unit
) : WebViewClient() {
    private var handled: Boolean = false

    // for API levels < 24
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return isUrlOverridden(view, Uri.parse(url))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return isUrlOverridden(view, request?.url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val uri = request?.url
            if(!handled && uri != null && uri.toString().contains(attempt.redirectUri)){
                handled = true
                javascriptCallback()
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        if(!handled && url?.contains(attempt.redirectUri) == true){
            handled = true
            javascriptCallback()
        }
        return super.shouldInterceptRequest(view, url)
    }

    private fun isUrlOverridden(view: WebView?, url: Uri?): Boolean {
        return when {
            url == null -> {
                false
            }
            url.toString().contains("appleid.apple.com") -> {
                view?.loadUrl(url.toString())
                true
            }
            url.toString().contains(attempt.redirectUri) -> {
                handleRedirect(url)

                true
            }
            else -> {
                false
            }
        }
    }

    private fun handleRedirect(url: Uri){
        Log.d(SIGN_IN_WITH_APPLE_LOG_TAG, "Web view was forwarded to redirect URI")

        val codeParameter = url.getQueryParameter("code")
        val stateParameter = url.getQueryParameter("state")

        when {
            codeParameter == null -> {
                callback(SignInWithAppleResult.Failure(IllegalArgumentException("code not returned")))
            }
            stateParameter != attempt.state -> {
                callback(SignInWithAppleResult.Failure(IllegalArgumentException("state does not match")))
            }
            else -> {
                handled = true
                callback(SignInWithAppleResult.Success(codeParameter))
            }
        }
    }

}
