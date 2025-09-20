package com.codeleg.momentum

import android.Manifest
import android.app.Dialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.codeleg.momentum.databinding.ActivityMainBinding
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlin.random.Random

class MainActivity : AppCompatActivity(), TodoItemInteractionListener {

    private lateinit var binding: ActivityMainBinding

    private val completedTodos = mutableListOf<ToDoModal>()
    private val activeTodos = mutableListOf<ToDoModal>()

    private lateinit var activeAdapter: ToDoAdapter
    private lateinit var completedAdapter: CompTodoAdapter

    private var dontAskAgainDelete = false
    private val TODO_INFO_CHANNEL_ID = "todo_info_channel"
    private val TODO_INFO_CHANNEL_NAME = "Todo Reminder"
    private  val NOTIFICATION_PERMISSION_REQUEST_CODE = 123

    private companion object {

        private val initialItems = listOf(
            ToDoModal("First Todo", 1, true),
            ToDoModal("Second Todo", 2, true),
            ToDoModal("Third Todo", 3, false),
            ToDoModal("Fourth Todo", 4, true),
            ToDoModal("Fifth Todo", 5, false),
            ToDoModal("Sixth Todo", 6, true),
            ToDoModal("Seventh Todo", 7, false),
            ToDoModal("Eighth Todo", 8, true),
            ToDoModal("Ninth Todo", 9, false),
            ToDoModal("Tenth Todo", 10, true),
            ToDoModal("Eleventh Todo", 11, false),
            ToDoModal("Twelfth Todo", 12, true),
            ToDoModal("Thirteenth Todo", 13, false),
            ToDoModal("Fourteenth Todo", 14, true),
            ToDoModal("Fifteenth Todo", 15, false),
            ToDoModal("Sixteenth Todo", 16, true),
            ToDoModal("Seventeenth Todo", 17, false),
            ToDoModal("Eighteenth Todo", 18, true),
            ToDoModal("Nineteenth Todo", 19, false),
            ToDoModal("Twentieth Todo", 20, true)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerViews()
        loadInitialData()
        setupFab()
        updateProgress()
        showNotification()

        binding.filterChips.setOnCheckedChangeListener { group, checkedId ->
            filterTodo(group, checkedId)
        }

        requestNotificationPermissionAndShow()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.activityMainToolbar)
    }

    private fun setupRecyclerViews() = with(binding) {
        todosRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            activeAdapter = ToDoAdapter(this@MainActivity, this@MainActivity, activeTodos)
            adapter = activeAdapter
        }
        completedTodoRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            completedAdapter = CompTodoAdapter(this@MainActivity, this@MainActivity, completedTodos)
            adapter = completedAdapter
        }
    }

    private fun setupFab() = with(binding) {
        addTodoDialogBtn.setOnClickListener { showAddTodoDialog() }
    }

    private fun loadInitialData() {
        initialItems.forEach { if (it.isDone) completedTodos.add(it) else activeTodos.add(it) }
        activeAdapter.notifyDataSetChanged()
        completedAdapter.notifyDataSetChanged()
        updateUIVisibility()
    }

    private fun showAddTodoDialog() {
        Dialog(this).apply {
            setContentView(R.layout.add_todo_layout)
            val titleInput = findViewById<EditText>(R.id.todo_title_input)
            findViewById<View>(R.id.add_todo_cancel_btn).setOnClickListener { dismiss() }
            findViewById<View>(R.id.add_todo_btn).setOnClickListener {
                val title = titleInput.text.toString().trim()
                if (title.isEmpty()) {
                    Snackbar.make(binding.root, "Title cannot be empty", Snackbar.LENGTH_SHORT)
                        .setAction("Dismiss") { }
                        .show()
                } else {
                    addTodo(ToDoModal(title, generateRandomId(), false))
                    dismiss()
                }
            }
            show()
        }
    }

    private fun addTodo(todo: ToDoModal) {
        activeTodos.add(todo)
        activeAdapter.notifyItemInserted(activeTodos.size - 1)
        binding.todosRecyclerView.scrollToPosition(activeTodos.size - 1)
        updateProgress()
        updateUIVisibility()
    }

    override fun onTodoItemChecked(item: ToDoModal, position: Int) {
        moveItemBetweenLists(activeTodos, completedTodos, position, activeAdapter, completedAdapter)
    }

    override fun onCompletedItemUnchecked(item: ToDoModal, position: Int) {
        moveItemBetweenLists(completedTodos, activeTodos, position, completedAdapter, activeAdapter)
    }

    private fun moveItemBetweenLists(
        from: MutableList<ToDoModal>,
        to: MutableList<ToDoModal>,
        pos: Int,
        fromAdapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>,
        toAdapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>
    ) {
        if (pos !in from.indices) return
        val item = from.removeAt(pos).apply { isDone = !isDone }
        fromAdapter.notifyItemRemoved(pos)
        to.add(item)
        toAdapter.notifyItemInserted(to.size - 1)
        updateProgress()
        updateUIVisibility()
    }

    override fun onAttemptDelete(callback: (Boolean) -> Unit) {
        if (dontAskAgainDelete) callback(true) else showConfirmDeleteDialog(callback)
        updateUIVisibility()
    }

    private fun showConfirmDeleteDialog(callback: (Boolean) -> Unit) {
        Dialog(this).apply {
            setContentView(R.layout.dialog_confirm_delete)
            val dontAsk = findViewById<CheckBox>(R.id.dont_ask_again_checkbox)
            findViewById<View>(R.id.delete_confirm_dialog_cancel_button).setOnClickListener {
                dismiss(); callback(false)
            }
            findViewById<View>(R.id.delete_todo_dialog_confirm_button).setOnClickListener {
                dontAskAgainDelete = dontAsk.isChecked
                dismiss(); callback(true)
            }
            show()
        }
    }

    private fun updateUIVisibility() = with(binding) {
        completedHeader.visibility = if (completedTodos.isNotEmpty()) View.VISIBLE else View.GONE
        completedTodoRecycler.visibility = completedHeader.visibility
        incompleteHeader.visibility = if (activeTodos.isNotEmpty()) View.VISIBLE else View.GONE
        todosRecyclerView.visibility = incompleteHeader.visibility
    }

    private fun updateProgress() = with(binding) {
        val total = activeTodos.size + completedTodos.size
        if (total == 0) {
            todoProgress.progress = 0
            todoCount.text = "Create a todo"
            return
        }
        val done = completedTodos.size
        todoProgress.progress = (done.toFloat() / total * 100).toInt()
        val todoCountText = "${total - done} active • $done done"
        todoCount.text = todoCountText
    }

    private fun generateRandomId() = Random.nextInt(100000, 999999)

    override fun onCreateOptionsMenu(menu: Menu?) = menuInflater.inflate(R.menu.activity_main_menu, menu).let { true }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_todo_option -> showAddTodoDialog()
            R.id.exit_option -> finishAffinity()
            R.id.settings_option -> Snackbar.make(binding.root, "This feature isn't available.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}.show()
            R.id.ask_question_option -> {
                val intent = Intent(Intent.ACTION_SEND)
                    .setType("message/rfc822")
                    .putExtra(Intent.EXTRA_EMAIL , arrayOf("shubhamgupta8609@gmail.com"))
                    .putExtra(Intent.EXTRA_SUBJECT , "Ask Question form Momentum")
                    .putExtra(Intent.EXTRA_TEXT , "Hello developers , I want to ask a question about your app Momentum..")
                 startActivity(Intent.createChooser(intent, "Send email using..."))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun filterTodo(group: ChipGroup, checkedId: Int) {
        when (checkedId) {
            R.id.chip_all -> {
                binding.incompleteHeader.visibility = if (activeTodos.isNotEmpty()) View.VISIBLE else View.GONE
                binding.todosRecyclerView.visibility = binding.incompleteHeader.visibility
                binding.completedHeader.visibility = if (completedTodos.isNotEmpty()) View.VISIBLE else View.GONE
                binding.completedTodoRecycler.visibility = binding.completedHeader.visibility
            }
            R.id.chip_active -> {
                binding.incompleteHeader.visibility = if (activeTodos.isNotEmpty()) View.VISIBLE else View.GONE
                binding.todosRecyclerView.visibility = binding.incompleteHeader.visibility
                binding.completedHeader.visibility = View.GONE
                binding.completedTodoRecycler.visibility = View.GONE
            }
            R.id.chip_completed -> {
                binding.completedHeader.visibility = if (completedTodos.isNotEmpty()) View.VISIBLE else View.GONE
                binding.completedTodoRecycler.visibility = binding.completedHeader.visibility
                binding.incompleteHeader.visibility = View.GONE
                binding.todosRecyclerView.visibility = View.GONE
            }
            else -> {
                binding.incompleteHeader.visibility = if (activeTodos.isNotEmpty()) View.VISIBLE else View.GONE
                binding.todosRecyclerView.visibility = binding.incompleteHeader.visibility
                binding.completedHeader.visibility = if (completedTodos.isNotEmpty()) View.VISIBLE else View.GONE
                binding.completedTodoRecycler.visibility = binding.completedHeader.visibility
            }
        }
    }

    private fun requestNotificationPermissionAndShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {

                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Snackbar.make(binding.root, "Notification permission is required to show alerts.", Snackbar.LENGTH_LONG)
                        .setAction("Grant") {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                NOTIFICATION_PERMISSION_REQUEST_CODE
                            )
                        }.show()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            // Permission not required for older versions, proceed to show notification
//            showAppNotification()
        }
    }


    private fun showNotification(){
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT  >= Build.VERSION_CODES.O){
            val reminderChannel = NotificationChannel(TODO_INFO_CHANNEL_ID , TODO_INFO_CHANNEL_NAME ,
                NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(reminderChannel)
        }

        val notificationBuilder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this , TODO_INFO_CHANNEL_ID)
        }else{
            Notification.Builder(this)
        }

        val pendingIntent = PendingIntent.getActivity(this , 0 , Intent(this , MainActivity::class.java) ,
            PendingIntent.FLAG_IMMUTABLE )

        notificationBuilder.setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Todo Reminder")
            .setContentText("Complete your todos and to release Dopaimine")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            val todoReminderNotification = notificationBuilder.build()
            val notificationId = 1;

        nm.notify(notificationId , todoReminderNotification )

    }


}

