package com.willowtreeapps.signinwithapplebutton.view
import kotlinx.serialization.Serializable

@Serializable
data class UserData(val name: UserName, val email: String?)

@Serializable
data class UserName(val firstName: String?, val middleName: String?, val lastName: String?)
