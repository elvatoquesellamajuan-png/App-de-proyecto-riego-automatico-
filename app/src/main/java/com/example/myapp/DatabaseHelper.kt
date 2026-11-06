package com.example.myapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "iot_database.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USERS = "usuarios"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER = "usuario"
        private const val COLUMN_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USER TEXT UNIQUE, " +
                "$COLUMN_PASSWORD TEXT)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun insertarUsuario(usuario: String, pass: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER, usuario)
            put(COLUMN_PASSWORD, pass)
        }
        val resultado = db.insert(TABLE_USERS, null, values)
        db.close()
        return resultado != -1L
    }

    fun verificarUsuario(usuario: String, pass: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_USER = ? AND $COLUMN_PASSWORD = ?", arrayOf(usuario, pass))
        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }
}