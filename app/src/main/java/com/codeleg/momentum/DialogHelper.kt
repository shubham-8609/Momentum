package com.codeleg.momentum

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.widget.CheckBox
import android.widget.EditText
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlin.random.Random

object DialogHelper {

    fun showAddTodoDialog(
        context: MainActivity,
        onAdd: (ToDoModal) -> Unit
    ) {
        Dialog(context).apply {
            setContentView(R.layout.add_todo_layout)
            val titleInput = findViewById<EditText>(R.id.todo_title_input)
            findViewById<View>(R.id.add_todo_cancel_btn).setOnClickListener { dismiss() }
            findViewById<View>(R.id.add_todo_btn).setOnClickListener {
                val title = titleInput.text.toString().trim()
                if (title.isEmpty()) {
                    Snackbar.make(context.binding.root, "Title cannot be empty", Snackbar.LENGTH_SHORT).show()
                } else {
                    onAdd(ToDoModal(title , generateRandomId() ,   false))
                    dismiss()
                }
            }
            show()
        }
    }

    fun showConfirmDeleteDialog(
        context: Context,
        pref: SharedPreferences,
        callback: (Boolean, Boolean) -> Unit
    ) {
        Dialog(context).apply {
            setContentView(R.layout.dialog_confirm_delete)
            val dontAsk = findViewById<CheckBox>(R.id.dont_ask_again_checkbox)

            findViewById<View>(R.id.delete_confirm_dialog_cancel_button).setOnClickListener {
                dismiss(); callback(false, false)
            }
            findViewById<View>(R.id.delete_todo_dialog_confirm_button).setOnClickListener {
                val dontAskChecked = dontAsk.isChecked
                pref.edit().putBoolean("delete_flag", dontAskChecked).apply()
                dismiss(); callback(true, dontAskChecked)
            }
            show()
        }
    }

    fun generateRandomId() = Random.nextInt(100000, 999999)
}
