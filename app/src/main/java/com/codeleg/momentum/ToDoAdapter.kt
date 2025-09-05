package com.codeleg.momentum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.helper.widget.Carousel
import androidx.recyclerview.widget.RecyclerView

class ToDoAdapter(private val dataList: MutableList<ToDoModal> ) : RecyclerView.Adapter<ToDoAdapter.ViewHolder>()  {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.to_do_item_layout , parent, false);
        return ViewHolder(view);

    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val todo = dataList[position]
        holder.titleText.text = todo.title

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = todo.isDone

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Update the isDone status in your data model
            todo.isDone = isChecked
            // Here you might want to notify your database or backend about the change
            // For now, we just update the local data
        }
        holder.deleteBtn.setOnClickListener {
            val currentPosition = holder.adapterPosition
            dataList.removeAt(currentPosition);
            notifyItemRemoved(currentPosition);
        }

    }

    override fun getItemCount(): Int  = dataList.size;

    inner  class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.title_view);
       val checkBox: CheckBox = itemView.findViewById(R.id.done_checkBox);
        val deleteBtn: ImageButton = itemView.findViewById(R.id.delete_btn);
    }



}