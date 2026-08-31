package com.callassistant.data.repository

import android.content.Context
import com.callassistant.util.CallNote
import com.callassistant.util.CallNotesStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NotesRepository {
    fun getAllNotes(): List<Pair<String, List<CallNote>>>
    fun getNotes(number: String): List<String>
    fun addNote(number: String, note: String)
}

@Singleton
class NotesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotesRepository {

    override fun getAllNotes(): List<Pair<String, List<CallNote>>> =
        CallNotesStore.getAllNotes(context)

    override fun getNotes(number: String): List<String> =
        CallNotesStore.getNotes(context, number)

    override fun addNote(number: String, note: String) {
        CallNotesStore.addNote(context, number, note)
    }
}
