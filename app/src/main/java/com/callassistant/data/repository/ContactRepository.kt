package com.callassistant.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.ContactSource
import com.callassistant.data.sync.ContactPhotoWriter
import com.callassistant.data.sync.ContactSyncer
import com.callassistant.util.DeletedEntriesStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>
    suspend fun syncContacts()
    suspend fun deleteContacts(contacts: List<Contact>)
    suspend fun saveContact(contact: Contact)
    suspend fun importContacts(contacts: List<Contact>)
}

@Singleton
class ContactRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val contactSyncer: ContactSyncer
) : ContactRepository {

    private fun contactKey(phoneNumber: String) = phoneNumber

    override fun observeContacts(): Flow<List<Contact>> = db.contactDao().getAll()

    override suspend fun syncContacts() {
        val deletedKeys = DeletedEntriesStore.getDeletedContactKeys(context)
        val localContacts = db.contactDao().getAll().first()
            .filter { it.source == ContactSource.LOCAL }
        val synced = contactSyncer.sync()
            .filterNot { contactKey(it.phoneNumber) in deletedKeys }
            .filterNot { contactKey(it.phoneNumber) in localContacts.map { contactKey(it.phoneNumber) } }
        db.contactDao().deleteAll()
        db.contactDao().insertAll(synced + localContacts)
    }

    override suspend fun deleteContacts(contacts: List<Contact>) {
        DeletedEntriesStore.addDeletedContactKeys(
            context,
            contacts.map { contactKey(it.phoneNumber) }
        )
        val resolver = context.contentResolver
        contacts.forEach { contact ->
            try {
                val encodedPhone = Uri.encode(contact.phoneNumber)
                val lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, encodedPhone
                )
                val seen = mutableSetOf<Long>()
                resolver.query(
                    lookupUri,
                    arrayOf(ContactsContract.PhoneLookup.CONTACT_ID),
                    null, null, null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID)
                    while (cursor.moveToNext()) {
                        val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                        if (seen.add(id)) {
                            resolver.delete(
                                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
                                null, null
                            )
                        }
                    }
                }
            } catch (_: SecurityException) {}
        }
        val updated = contactSyncer.sync()
        db.contactDao().deleteAll()
        db.contactDao().insertAll(updated)
    }

    override suspend fun saveContact(contact: Contact) {
        val toSave = if (
            contact.photoUri != null ||
            !contact.address.isNullOrBlank() ||
            contact.latitude != null ||
            contact.longitude != null
        ) {
            contact.copy(source = ContactSource.LOCAL)
        } else {
            contact
        }
        db.contactDao().insertAll(listOf(toSave))
        ContactPhotoWriter.writeName(
            context, toSave.phoneNumber, toSave.name
        )
        toSave.photoUri?.let { photoUri ->
            ContactPhotoWriter.writePhoto(
                context, toSave.phoneNumber, toSave.name, photoUri
            )
        }
    }

    override suspend fun importContacts(contacts: List<Contact>) {
        db.contactDao().insertAll(contacts)
    }
}
