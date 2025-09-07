package com.codeleg.momentum

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.codeleg.momentum.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity(), TodoItemInteractionListener {
    private lateinit var inCompletedTodoRecycler: RecyclerView
    private lateinit var binding: ActivityMainBinding

    private val completedTodoItems: MutableList<ToDoModal> = mutableListOf()
    private val incompletedTodoItems: MutableList<ToDoModal> = mutableListOf()
    private lateinit var toDoAdapter: ToDoAdapter
    private lateinit var completedTodoAdapter: CompTodoAdapter
    private lateinit var addTodoBtn: FloatingActionButton
    private lateinit var completedTodoRecycler: RecyclerView
    private lateinit var completedTodoHeading: TextView
    private lateinit var inCompletedTodoHeading: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        addTodoBtn = binding.addTodoDialogBtn

        // Initialize ToDo RecyclerView
        inCompletedTodoRecycler = binding.todosRecyclerView
        inCompletedTodoRecycler.layoutManager = LinearLayoutManager(this)
        toDoAdapter = ToDoAdapter(this, this, incompletedTodoItems)
        inCompletedTodoRecycler.adapter = toDoAdapter

        // Initialize Completed Todo RecyclerView
        completedTodoRecycler = binding.completedTodoRecycler
        completedTodoRecycler.layoutManager = LinearLayoutManager(this)
        completedTodoAdapter = CompTodoAdapter(this, this, completedTodoItems)
        completedTodoRecycler.adapter = completedTodoAdapter
        completedTodoHeading = binding.completedTodoHeading
        inCompletedTodoHeading = binding.incompletedTodoHeading

        distributeInitialData()

        addTodoBtn.setOnClickListener {
            showAddTodoDialog()
        }
    }

    private fun showAddTodoDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.add_todo_layout)
        val editTextTitle = dialog.findViewById<EditText>(R.id.todo_title_input)
        val addButton: AppCompatButton = dialog.findViewById(R.id.add_todo_btn)

        addButton.setOnClickListener {
            val newTodoTitle = editTextTitle?.text.toString().trim()
            if (newTodoTitle.isNotEmpty()) {
                val newTodo = ToDoModal(newTodoTitle, generateRandomId(), false)
                incompletedTodoItems.add(newTodo)
                toDoAdapter.notifyItemInserted(incompletedTodoItems.size - 1)
                inCompletedTodoRecycler.scrollToPosition(incompletedTodoItems.size - 1)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun distributeInitialData() {
        val allItems = listOf(
            ToDoModal("First Todo ", 1, true),
            ToDoModal("Second Todo ", 2, true),
            ToDoModal("Third Todo ", 3, false),
            ToDoModal("Fourth Todo ", 4, true),
            ToDoModal("Fifth Todo ", 5, false),
            ToDoModal("Sixth Todo ", 6, true),
            ToDoModal("Seventh Todo ", 7, false),
            ToDoModal("Eighth Todo ", 8, true),
            ToDoModal("Ninth Todo ", 9, false),
            ToDoModal("Tenth Todo ", 10, true),
            ToDoModal("Eleventh Todo ", 11, false),
            ToDoModal("Twelfth Todo ", 12, true),
            ToDoModal("Thirteenth Todo ", 13, false),
            ToDoModal("Fourteenth Todo ", 14, true),
            ToDoModal("Fifteenth Todo ", 15, false),
            ToDoModal("Sixteenth Todo ", 16, true),
            ToDoModal("Seventeenth Todo ", 17, false),
            ToDoModal("Eighteenth Todo ", 18, true),
            ToDoModal("Nineteenth Todo ", 19, false),
            ToDoModal("Twentieth Todo ", 20, true)
        )
        allItems.forEach { item ->
            if (item.isDone) {
                completedTodoItems.add(item)
            } else {
                incompletedTodoItems.add(item)
            }
        }
        toDoAdapter.notifyDataSetChanged()
        completedTodoAdapter.notifyDataSetChanged()
    }


    private fun generateRandomId(): Int {
        return Random.nextInt(100000, 999999)
    }

    override fun onTodoItemChecked(item: ToDoModal, position: Int) {
        // Ensure position is valid before removing
        if (position < 0 || position >= incompletedTodoItems.size) {
            // Log an error or handle gracefully
            return
        }
        incompletedTodoItems.removeAt(position)
        toDoAdapter.notifyItemRemoved(position)

        
        item.isDone = true
        completedTodoItems.add(item)
        completedTodoAdapter.notifyItemInserted(completedTodoItems.size - 1)
        completedTodoRecycler.scrollToPosition(completedTodoItems.size-1)
        checkLayout()
    }

    override fun onCompletedItemUnchecked(item: ToDoModal, position: Int) {
        // Ensure position is valid before removing
        if (position < 0 || position >= completedTodoItems.size) {
            // Log an error or handle gracefully
            return
        }
        completedTodoItems.removeAt(position)
        completedTodoAdapter.notifyItemRemoved(position)

        item.isDone = false
        incompletedTodoItems.add(item)
        toDoAdapter.notifyItemInserted(incompletedTodoItems.size - 1)
        inCompletedTodoRecycler.scrollToPosition(incompletedTodoItems.size - 1)
        checkLayout();

    }
    fun checkLayout(){
        if (completedTodoItems.isEmpty()){
            completedTodoHeading.visibility = View.GONE
            completedTodoRecycler.visibility = View.GONE
        }else if( !completedTodoItems.isEmpty()){
            completedTodoHeading.visibility = View.VISIBLE
            completedTodoRecycler.visibility = View.VISIBLE



        }
        if(incompletedTodoItems.isEmpty()){
            inCompletedTodoHeading.visibility = View.GONE
            inCompletedTodoRecycler.visibility = View.GONE

        }else if(!incompletedTodoItems.isEmpty()){
            inCompletedTodoHeading.visibility = View.VISIBLE
            inCompletedTodoRecycler.visibility = View.VISIBLE
        }
    }
}
