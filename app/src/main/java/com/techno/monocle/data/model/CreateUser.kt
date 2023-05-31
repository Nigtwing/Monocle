package com.techno.monocle.data.model

data class CreateUser(
    var displayName: String = "",
    var email: String = "",
    var password: String = ""
)