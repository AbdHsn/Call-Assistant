package com.callassistant.data.sync

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.ContactSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactSyncer(private val context: Context) {

    suspend fun sync(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val resolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val number = if (numberIndex >= 0) {
                    cursor.getString(numberIndex)?.replace(" ", "")?.trim()
                } else null
                number ?: continue

                val rawName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    ?: if (displayNameIndex >= 0) cursor.getString(displayNameIndex) else null
                val name = rawName?.trim()?.ifBlank { number } ?: number
                contacts.add(Contact(name = name, phoneNumber = number, source = ContactSource.IMPORTED))
            }
        }
        contacts
    }
}
