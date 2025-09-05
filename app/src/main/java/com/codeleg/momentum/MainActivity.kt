package com.codeleg.momentum

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codeleg.momentum.ToDoAdapter
import com.codeleg.momentum.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var toDoRecyclerView: RecyclerView ;
    private lateinit var binding: ActivityMainBinding
    private lateinit var dataList: MutableList<ToDoModal>;
    private lateinit var toDoAdapter: ToDoAdapter;
    private lateinit var addTodoBtn: FloatingActionButton;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater);
        setContentView(binding.root);



        ViewCompat.setOnApplyWindowInsetsListener(
            binding.main
        ) { v: View, insets: WindowInsetsCompat ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        addTodoBtn = binding.addTodoDialogBtn;
        dataList = mutableListOf();
        addData();
        toDoRecyclerView = binding.todosRecyclerView;
        toDoRecyclerView.layoutManager = LinearLayoutManager(this);
        toDoAdapter = ToDoAdapter(dataList)
        toDoRecyclerView.adapter = toDoAdapter;

        addTodoBtn.setOnClickListener {
            val dialog: Dialog = Dialog(this);
            dialog.setContentView(R.layout.add_todo_layout)
            val editTextTitle = dialog.findViewById<EditText>(R.id.todo_title_input)
            val addButton: AppCompatButton = dialog.findViewById(R.id.add_todo_btn)
            addButton.setOnClickListener {
                val newTodoTitle = editTextTitle?.text.toString();
                if (newTodoTitle.isNotEmpty() && editTextTitle != null) {
                    val newTodo = ToDoModal(newTodoTitle, generateRandomId().toLong(), false)
                    dataList.add(newTodo)
                    toDoAdapter.notifyItemInserted(dataList.size - 1)
                    dialog.dismiss()
                }
            }

            dialog.show()


        }
    }
    private fun addData() {

        dataList.add(ToDoModal("first Todo " , 232534 , true));
        dataList.add(ToDoModal("second Todo " , 232535 , true));
        dataList.add(ToDoModal("third Todo " , 232536 , false));
        dataList.add(ToDoModal("fourth Todo " , 232537 , true));
        dataList.add(ToDoModal("fifth Todo " , 232538 , false));
        dataList.add(ToDoModal("sixth Todo " , 232539 , true));
        dataList.add(ToDoModal("seventh Todo " , 232540 , false));
        dataList.add(ToDoModal("eighth Todo " , 232541 , true));
        dataList.add(ToDoModal("ninth Todo " , 232542 , false));
        dataList.add(ToDoModal("tenth Todo " , 232543 , true));
        dataList.add(ToDoModal("eleventh Todo " , 232544 , false));
        dataList.add(ToDoModal("twelfth Todo " , 232545 , true));
        dataList.add(ToDoModal("thirteenth Todo " , 232546 , false));
        dataList.add(ToDoModal("fourteenth Todo " , 232547 , true));
        dataList.add(ToDoModal("fifteenth Todo " , 232548 , false));
        dataList.add(ToDoModal("sixteenth Todo " , 232549 , true));
        dataList.add(ToDoModal("seventeenth Todo " , 232550 , false));
        dataList.add(ToDoModal("eighteenth Todo " , 232551 , true));
        dataList.add(ToDoModal("nineteenth Todo " , 232552 , false));
        dataList.add(ToDoModal("twentieth Todo " , 232553 , true));
    }

    private fun generateRandomId(): Int {
        return Random.nextInt(100000, 999999)
    }

}