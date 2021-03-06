package com.willowtreeapps.signinwithapplebutton.view

import android.webkit.JavascriptInterface
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleConfiguration
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleResult
import com.willowtreeapps.signinwithapplebutton.view.FormInterceptorInterface.Companion.CODE
import com.willowtreeapps.signinwithapplebutton.view.FormInterceptorInterface.Companion.JS_TO_INJECT
import com.willowtreeapps.signinwithapplebutton.view.FormInterceptorInterface.Companion.NAME
import com.willowtreeapps.signinwithapplebutton.view.FormInterceptorInterface.Companion.STATE
import kotlinx.serialization.json.Json

/**
 * Original author https://github.com/pedrodanielcsantos
 */

/**
 * [JavascriptInterface] to be injected in a WebView (with name [NAME], see [WebView.addJavascriptInterface]) that
 * receives the "form_data" from a web page (triggered by [JS_TO_INJECT]) and analyzes the 2 expected fields from apple
 * authentication:
 * - [STATE] : a _nonce_ string set in [AuthenticationAttempt.create] that needs to match [expectedState];
 * - [CODE] : the authorization code that'll be used to authenticate the user.
 */
class FormInterceptorInterface(
    private val responseType: String,
    private val expectedState: String,
    private val callback: (SignInWithAppleResult) -> Unit
) {
    @JavascriptInterface
    fun processFormData(formData: String) {
        val values = formData.split(FORM_DATA_SEPARATOR)
        val codeKey =
            if (responseType == SignInWithAppleConfiguration.RESPONSE_ID_TOKEN) ID_TOKEN else CODE
        val codeEncoded = values.find { it.startsWith(codeKey) }
        val stateEncoded = values.find { it.startsWith(STATE) }

        if (codeEncoded != null && stateEncoded != null) {
            val stateValue = stateEncoded.substringAfter(KEY_VALUE_SEPARATOR)
            val codeValue = codeEncoded.substringAfter(KEY_VALUE_SEPARATOR)

            if (stateValue == expectedState) {
                // Success, authorizationCode is codeValue
                val userEncoded = values.find { it.startsWith(USER) }
                val success = if (userEncoded != null) {
                    val userValue = userEncoded.substringAfter(KEY_VALUE_SEPARATOR)
                    val json = Json(from = Json.Default) {
                        encodeDefaults = true
                        allowStructuredMapKeys = true
                    }
                    val userData = json.decodeFromString(UserData.serializer(), userValue)
                    SignInWithAppleResult.Success(
                        codeValue,
                        userData.email,
                        userData.name.firstName,
                        userData.name.lastName
                    )
                } else {
                    SignInWithAppleResult.Success(codeValue)
                }
                callback(success)
            } else {
                // Error, state doesn't match.
                callback(SignInWithAppleResult.Failure(IllegalArgumentException("state does not match")))
            }
        } else {
            // Error, couldn't find the required info.
            callback(SignInWithAppleResult.Failure(IllegalArgumentException("code not returned")))
        }
    }

    companion object {
        const val NAME = "FormInterceptorInterface"
        private const val STATE = "state"
        private const val CODE = "code"
        private const val ID_TOKEN = "id_token"
        private const val USER = "user"
        private const val FORM_DATA_SEPARATOR = "|"
        private const val KEY_VALUE_SEPARATOR = "="

        /**
         * This piece of Javascript code fetches all (key, value) attributes from the site's form data and concatenates
         * them in the form: "key [KEY_VALUE_SEPARATOR] value [FORM_DATA_SEPARATOR]".
         * Then, invokes the method [processFormData] on the app's side (that's exposed to Javascript) so that the form
         * data can be analyzed in the app's context.
         */
        val JS_TO_INJECT = """
        function parseForm(form){
            var values = '';
            for(var i=0 ; i< form.elements.length; i++){
                values += 
                    form.elements[i].name + 
                    '$KEY_VALUE_SEPARATOR' + 
                    form.elements[i].value + 
                    '$FORM_DATA_SEPARATOR'
            }
            window.$NAME.processFormData(values);
        }
        
        for(var i=0 ; i< document.forms.length ; i++){
            parseForm(document.forms[i]);
        }
        """.trimIndent()
    }
}
