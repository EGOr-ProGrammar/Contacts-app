package com.egorroman.contactsapp.presentation

import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egorroman.contactsapp.domain.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel : ViewModel() {

    private val _state = MutableStateFlow(ContactsState())
    val state = _state.asStateFlow()

    fun loadContacts(contentResolver: ContentResolver) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                errorMessage = null,
                isPermissionRequired = false
            )

            try {
                val contacts = fetchContacts(contentResolver)
                val grouped = contacts.groupBy { it.initial }
                _state.value = _state.value.copy(contacts = grouped, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = e.message ?: "Ошибка при загрузке контактов",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun fetchContacts(contentResolver: ContentResolver): List<Contact> =
        withContext(Dispatchers.IO) {
            val contactsMap = mutableMapOf<String, Contact>()

            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val idIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex) ?: continue

                    if (contactsMap.containsKey(id)) {
                        continue
                    }

                    val fullName = it.getString(nameIndex)?.trim() ?: "Неизвестно"
                    val number = it.getString(numberIndex) ?: ""

                    val spaceIndex = fullName.indexOf(' ')
                    val firstName =
                        if (spaceIndex != -1) fullName.substring(0, spaceIndex) else fullName
                    val lastName =
                        if (spaceIndex != -1) fullName.substring(spaceIndex + 1).trimStart() else ""

                    val initial =
                        fullName.firstOrNull { char -> char.isLetter() }?.uppercaseChar() ?: '#'

                    contactsMap[id] = Contact(
                        id = id,
                        firstName = firstName,
                        lastName = lastName,
                        phone = number,
                        initial = initial
                    )
                }
            }

            contactsMap.values.sortedBy { it.firstName }
        }
}
