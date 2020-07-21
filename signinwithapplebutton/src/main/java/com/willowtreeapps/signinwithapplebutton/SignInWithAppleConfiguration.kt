package com.willowtreeapps.signinwithapplebutton

data class SignInWithAppleConfiguration(
    val clientId: String,
    val redirectUri: String,
    val scope: String,
    val responseType: String
) {

    class Builder {
        private lateinit var clientId: String
        private lateinit var redirectUri: String
        private lateinit var scope: String
        private lateinit var responseType: String

        fun clientId(clientId: String) = apply {
            this.clientId = clientId
        }

        fun redirectUri(redirectUri: String) = apply {
            this.redirectUri = redirectUri
        }

        fun scope(scope: String) = apply {
            this.scope = scope
        }

        fun responseType(responseType: String) = apply {
            this.responseType = responseType
        }

        fun build() = SignInWithAppleConfiguration(clientId, redirectUri, scope, responseType)
    }

    companion object {
        const val RESPONSE_CODE = "code"
        const val RESPONSE_ID_TOKEN = "code id_token"
    }
}
