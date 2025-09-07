package com.codeleg.momentum

interface TodoItemInteractionListener {
    fun onTodoItemChecked(item: ToDoModal, position: Int)
    fun onCompletedItemUnchecked(item: ToDoModal, position: Int)
}
