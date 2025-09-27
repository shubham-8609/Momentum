package com.codeleg.momentum

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE $TODO_TABLE (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title VARCHAR(100) NOT NULL, " +
                    "isDone BOOLEAN DEFAULT FALSE" +
                    ");"
        )

    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("DROP TABLE IF EXISTS $TODO_TABLE")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "momentum.db"
        private const val DATABASE_VERSION = 1
        private const val TODO_TABLE = "todos"
    }
    fun addTodo(title: String){
    val db =  this.writableDatabase
        val values = ContentValues()
            values.put("title" ,  title)
        db.apply {
            insert(TODO_TABLE , null , values )
        }
    }

    fun fetchData(): ArrayList<ToDoModal>{
        val  restoredTodos = ArrayList<ToDoModal>()
        val db = this.readableDatabase
        val cursor  = db.rawQuery("SELECT * FROM $TODO_TABLE" , null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val isDone = cursor.getInt(cursor.getColumnIndexOrThrow("isDone")) == 1

            restoredTodos.add(ToDoModal(title ,id ,  isDone)) // ✅ add to list
        }


        cursor.close()
        return restoredTodos
    }

    fun update(todo : ToDoModal , title: String? = null , isChecked: Boolean? = null){
        val db = this.writableDatabase
        val cv = ContentValues()
        if(title == null){
            isChecked?.let { cv.put("isDone", if (it) 1 else 0) }

            db.update(TODO_TABLE , cv , "id = ${todo.id}" , null)
        }else{
        cv.put("title" ,title  )
            db.update(TODO_TABLE , cv , "id = ${todo.id}" , null )
        }



    }

    fun delete(givenId:Int){

        val db = this.writableDatabase
        db.delete(TODO_TABLE , "id = $givenId" , null )

    }

}
