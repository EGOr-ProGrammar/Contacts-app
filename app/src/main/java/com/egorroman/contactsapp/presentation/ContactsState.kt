package com.egorroman.contactsapp.presentation

import com.egorroman.contactsapp.domain.Contact

data class ContactsState(
    val contacts: Map<Char, List<Contact>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPermissionRequired: Boolean = true,
)
