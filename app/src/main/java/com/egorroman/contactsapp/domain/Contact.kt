package com.egorroman.contactsapp.domain

data class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val initial: Char
)