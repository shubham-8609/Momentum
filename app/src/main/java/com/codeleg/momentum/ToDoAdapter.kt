package com.codeleg.momentum

import android.app.Dialog
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox

import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar

class ToDoAdapter(
    private val context: Context,
    private val listener: TodoItemInteractionListener,
    private val dataList: MutableList<ToDoModal>,
    private val DBHelper: DatabaseHelper
) : RecyclerView.Adapter<ToDoAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.to_do_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val todo = dataList[position]
        holder.titleText.text = todo.title

        // Temporarily remove listener to prevent it firing during programmatic setChecked
        holder.checkBox.setOnCheckedChangeListener(null)
        // Set the state based on the model (should be false for active todos)
        holder.checkBox.isChecked = todo.isDone
        updateStrikeThrough(holder.titleText, todo.isDone)

        // Set the listener for user interactions
        holder.checkBox.setOnCheckedChangeListener { _, isCheckedByUser ->
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) {
                return@setOnCheckedChangeListener
            }
            val currentItem = dataList[currentPosition]

            if (isCheckedByUser) { // If the user checks the item
                listener.onTodoItemChecked(currentItem, currentPosition)
            }
            // If !isCheckedByUser, user unchecks an active todo. It remains active.
            // No state change or list transfer action is needed here from this adapter.
            // The MainActivity handles the logic if an item is moved.
        }

        holder.deleteBtn.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            listener.onAttemptDelete { confirmed ->
                if (confirmed) {
                    dataList.removeAt(currentPosition)
                    DBHelper.delete(todo.id)
                    notifyItemRemoved(currentPosition)
                }
            }
        }

        holder.editBtn.setOnClickListener {
            editLogic(holder)
        }
        holder.titleText.setOnClickListener {
            editLogic(holder)
        }


    }


    fun editLogic(holder: ToDoAdapter.ViewHolder){
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.edit_todo_layout)
        val editTodoTitleTextView: TextView = dialog.findViewById(R.id.edit_todo_title_input)
        val editTodoSaveBtn: AppCompatButton = dialog.findViewById(R.id.save_todo_btn)
        val editTodoCancelBtn: AppCompatButton = dialog.findViewById(R.id.cancel_edit_btn)

        val currentPosition = holder.adapterPosition
        if (currentPosition == RecyclerView.NO_POSITION) {
            dialog.dismiss()

        }
        editTodoTitleTextView.text = dataList[currentPosition].title

        editTodoSaveBtn.setOnClickListener {

            if(checkEmpty(editTodoTitleTextView)){
                val updatedTitle = editTodoTitleTextView.text.toString()
                val itemPosition = holder.adapterPosition // Re-fetch position, could change
                if (itemPosition != RecyclerView.NO_POSITION) {
                    dataList[itemPosition].title = updatedTitle
                    DBHelper.update(dataList[itemPosition] , title = updatedTitle)
                    notifyItemChanged(itemPosition)
                }
                dialog.dismiss()
            }else{
                Snackbar.make(holder.itemView , "Todo cannot be empty.." , Snackbar.LENGTH_SHORT).show()
            }


        }
        editTodoCancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    override fun getItemCount(): Int = dataList.size

    private fun updateStrikeThrough(textView: TextView, isChecked: Boolean) {
        if (isChecked) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
    fun checkEmpty(textView: TextView):Boolean{
        return textView.text.toString().trim().isNotEmpty()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.title_view)
        val checkBox: CheckBox = itemView.findViewById(R.id.done_checkBox)
        val deleteBtn: ShapeableImageView = itemView.findViewById(R.id.delete_btn)
        val editBtn: ShapeableImageView = itemView.findViewById(R.id.edit_btn)
    }

}
