package com.example.voxa.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.voxa.data.model.Conversation
import com.example.voxa.data.model.Message
import com.example.voxa.data.model.MessageDirection
import com.example.voxa.data.model.MessageStatus
import com.example.voxa.data.model.MessageType

class VoxaDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CONVERSATIONS (
                $COL_CONV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CONV_NAME TEXT NOT NULL,
                $COL_CONV_NUMBER TEXT NOT NULL UNIQUE,
                $COL_CONV_PINNED INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_MUTED INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_ARCHIVED INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_DELETED INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_DELETED_AT INTEGER,
                $COL_CONV_KEEP_ARCHIVED INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_UNREAD INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_BLOCKED INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_LAST_TEXT TEXT NOT NULL DEFAULT '',
                $COL_CONV_LAST_TIME INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_LAST_DIR TEXT NOT NULL DEFAULT 'in'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MSG_CONV_ID INTEGER NOT NULL,
                $COL_MSG_DIR TEXT NOT NULL,
                $COL_MSG_TEXT TEXT NOT NULL,
                $COL_MSG_TIME INTEGER NOT NULL,
                $COL_MSG_STARRED INTEGER NOT NULL DEFAULT 0,
                $COL_MSG_STATUS TEXT NOT NULL,
                $COL_MSG_SCHEDULED_TIME INTEGER,
                $COL_MSG_TYPE TEXT NOT NULL DEFAULT 'TEXT',
                $COL_MSG_DATA_URI TEXT,
                $COL_MSG_FILE_NAME TEXT,
                $COL_MSG_FILE_SIZE TEXT,
                $COL_MSG_CONTACT_NAME TEXT,
                $COL_MSG_CONTACT_NUMBER TEXT,
                FOREIGN KEY ($COL_MSG_CONV_ID) REFERENCES $TABLE_CONVERSATIONS($COL_CONV_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONVERSATIONS")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        purgeDemoData(db)
    }

    fun purgeDemoData(db: SQLiteDatabase) {
        try {
            val demoNumbers = arrayOf(
                "+255 712 345 678",
                "+255 655 210 992",
                "+255 788 004 411",
                "+255 621 887 034"
            )
            val placeholders = demoNumbers.joinToString(",") { "?" }
            db.execSQL(
                "DELETE FROM $TABLE_MESSAGES WHERE $COL_MSG_CONV_ID IN (SELECT $COL_CONV_ID FROM $TABLE_CONVERSATIONS WHERE $COL_CONV_NUMBER IN ($placeholders))",
                demoNumbers
            )
            db.execSQL(
                "DELETE FROM $TABLE_CONVERSATIONS WHERE $COL_CONV_NUMBER IN ($placeholders)",
                demoNumbers
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun insertConv(
        db: SQLiteDatabase,
        name: String,
        number: String,
        pinned: Boolean,
        muted: Boolean,
        archived: Boolean,
        deleted: Boolean,
        unread: Boolean,
        lastText: String,
        lastTime: Long,
        lastDir: String
    ): Long {
        val cv = ContentValues().apply {
            put(COL_CONV_NAME, name)
            put(COL_CONV_NUMBER, number)
            put(COL_CONV_PINNED, if (pinned) 1 else 0)
            put(COL_CONV_MUTED, if (muted) 1 else 0)
            put(COL_CONV_ARCHIVED, if (archived) 1 else 0)
            put(COL_CONV_DELETED, if (deleted) 1 else 0)
            put(COL_CONV_KEEP_ARCHIVED, 0)
            put(COL_CONV_UNREAD, if (unread) 1 else 0)
            put(COL_CONV_BLOCKED, 0)
            put(COL_CONV_LAST_TEXT, lastText)
            put(COL_CONV_LAST_TIME, lastTime)
            put(COL_CONV_LAST_DIR, lastDir)
        }
        return db.insert(TABLE_CONVERSATIONS, null, cv)
    }

    private fun insertMsg(
        db: SQLiteDatabase,
        convId: Long,
        dir: MessageDirection,
        text: String,
        time: Long,
        starred: Boolean,
        status: MessageStatus
    ): Long {
        val cv = ContentValues().apply {
            put(COL_MSG_CONV_ID, convId)
            put(COL_MSG_DIR, dir.name)
            put(COL_MSG_TEXT, text)
            put(COL_MSG_TIME, time)
            put(COL_MSG_STARRED, if (starred) 1 else 0)
            put(COL_MSG_STATUS, status.name)
            put(COL_MSG_TYPE, MessageType.TEXT.name)
        }
        return db.insert(TABLE_MESSAGES, null, cv)
    }

    companion object {
        const val DATABASE_NAME = "voxa_messages.db"
        const val DATABASE_VERSION = 2

        const val TABLE_CONVERSATIONS = "conversations"
        const val COL_CONV_ID = "id"
        const val COL_CONV_NAME = "name"
        const val COL_CONV_NUMBER = "number"
        const val COL_CONV_PINNED = "is_pinned"
        const val COL_CONV_MUTED = "is_muted"
        const val COL_CONV_ARCHIVED = "is_archived"
        const val COL_CONV_DELETED = "is_deleted"
        const val COL_CONV_DELETED_AT = "deleted_at"
        const val COL_CONV_KEEP_ARCHIVED = "keep_archived"
        const val COL_CONV_UNREAD = "unread"
        const val COL_CONV_BLOCKED = "is_blocked"
        const val COL_CONV_LAST_TEXT = "last_text"
        const val COL_CONV_LAST_TIME = "last_time"
        const val COL_CONV_LAST_DIR = "last_dir"

        const val TABLE_MESSAGES = "messages"
        const val COL_MSG_ID = "id"
        const val COL_MSG_CONV_ID = "conversation_id"
        const val COL_MSG_DIR = "dir"
        const val COL_MSG_TEXT = "text"
        const val COL_MSG_TIME = "time"
        const val COL_MSG_STARRED = "is_starred"
        const val COL_MSG_STATUS = "status"
        const val COL_MSG_SCHEDULED_TIME = "scheduled_time"
        const val COL_MSG_TYPE = "type"
        const val COL_MSG_DATA_URI = "data_uri"
        const val COL_MSG_FILE_NAME = "file_name"
        const val COL_MSG_FILE_SIZE = "file_size"
        const val COL_MSG_CONTACT_NAME = "contact_name"
        const val COL_MSG_CONTACT_NUMBER = "contact_number"
    }
}
