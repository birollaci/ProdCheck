package com.example.prodcheck

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.prodcheck.DataManager.nrScansDB
import com.example.prodcheck.DataManager.nrValidScansDB

// database class
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "prodcheck", null, 1) {
    val TABLE_NAME = "barcodes"
    val COLUMN_ID = "id"
    val COLUMN_NAME1 = "barcode"
    val COLUMN_NAME2 = "status"
    // schema
    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_NAME1 TEXT, $COLUMN_NAME2 TEXT)"
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    // adatok beszúrása
    fun addData(data: Data) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_NAME1, data.barcode)
        values.put(COLUMN_NAME2, data.status)
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun select() {
        val db = this.writableDatabase
        nrScansDB = 0
        nrValidScansDB = 0
        val cursor = db.query("barcodes",null, null, null, null, null, null)
        while (cursor.moveToNext()) {
//            val id = cursor.getInt(cursor.getColumnIndex("id"))
//            val barcode = cursor.getString(cursor.getColumnIndex("barcode"))
            val status = cursor.getString(cursor.getColumnIndex("status"))

            nrScansDB++
            if(status.toString() == "OK"){
                nrValidScansDB++
            }
        }
    }

    // data model
    data class Data(val barcode: String, val status: String)
}