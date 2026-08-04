package com.callassistant.data.sync

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Writes contact photos into the system Contacts Provider so they survive
 * app uninstall/reinstall the same way the underlying contact does, instead
 * of only living in this app's private storage.
 */
object ContactPhotoWriter {

    fun writePhoto(context: Context, phoneNumber: String, displayName: String, photoUri: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val resolver = context.contentResolver
        val photoBytes = loadPhotoBytes(context, photoUri) ?: return false

        return try {
            val rawContactId = findRawContactId(resolver, phoneNumber)
            if (rawContactId != null) {
                writePhotoForRawContact(resolver, rawContactId, photoBytes)
            } else {
                createContactWithPhoto(resolver, displayName, phoneNumber, photoBytes)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun loadPhotoBytes(context: Context, photoUri: String): ByteArray? {
        return try {
            context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private fun findRawContactId(resolver: ContentResolver, phoneNumber: String): Long? {
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        var contactId: Long? = null
        resolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) contactId = cursor.getLong(0)
        }
        val id = contactId ?: return null

        var rawContactId: Long? = null
        resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) rawContactId = cursor.getLong(0)
        }
        return rawContactId
    }

    private fun findPhotoDataId(resolver: ContentResolver, rawContactId: Long): Long? {
        var id: Long? = null
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) id = cursor.getLong(0)
        }
        return id
    }

    private fun writePhotoForRawContact(resolver: ContentResolver, rawContactId: Long, photoBytes: ByteArray) {
        val existingDataId = findPhotoDataId(resolver, rawContactId)
        val values = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
        }
        if (existingDataId != null) {
            resolver.update(
                ContactsContract.Data.CONTENT_URI,
                values,
                "${ContactsContract.Data._ID} = ?",
                arrayOf(existingDataId.toString())
            )
        } else {
            resolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }

    private fun createContactWithPhoto(
        resolver: ContentResolver,
        displayName: String,
        phoneNumber: String,
        photoBytes: ByteArray
    ) {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                .build()
        )
        resolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    fun writeName(context: Context, phoneNumber: String, displayName: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val resolver = context.contentResolver
        return try {
            val rawContactId = findRawContactId(resolver, phoneNumber)
            if (rawContactId != null) {
                writeNameForRawContact(resolver, rawContactId, displayName)
            } else {
                createContactWithName(resolver, displayName, phoneNumber)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findNameDataId(resolver: ContentResolver, rawContactId: Long): Long? {
        var id: Long? = null
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) id = cursor.getLong(0)
        }
        return id
    }

    private fun writeNameForRawContact(resolver: ContentResolver, rawContactId: Long, displayName: String) {
        val existingDataId = findNameDataId(resolver, rawContactId)
        val values = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName)
        }
        if (existingDataId != null) {
            resolver.update(
                ContactsContract.Data.CONTENT_URI,
                values,
                "${ContactsContract.Data._ID} = ?",
                arrayOf(existingDataId.toString())
            )
        } else {
            resolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }

    private fun createContactWithName(
        resolver: ContentResolver,
        displayName: String,
        phoneNumber: String
    ) {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )
        resolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }
}
