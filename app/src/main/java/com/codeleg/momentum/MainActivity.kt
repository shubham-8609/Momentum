package com.codeleg.momentum

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.codeleg.momentum.DialogHelper.showAddTodoDialog
import com.codeleg.momentum.databinding.ActivityMainBinding
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlin.random.Random

sealed class TodoFilter(val key: String) {
    object All : TodoFilter("all")
    object Active : TodoFilter("active")
    object Completed : TodoFilter("completed")

    companion object {
        fun fromKey(key: String): TodoFilter = when (key) {
            "active" -> Active
            "completed" -> Completed
            else -> All
        }
    }
}

class MainActivity : AppCompatActivity(), TodoItemInteractionListener {

      lateinit var binding: ActivityMainBinding
    private lateinit var activeAdapter: ToDoAdapter
    private lateinit var completedAdapter: CompTodoAdapter
    private lateinit var nm: NotificationManager
    private lateinit var pref: SharedPreferences

    private val activeTodos = mutableListOf<ToDoModal>()
    private val completedTodos = mutableListOf<ToDoModal>()
    private var dontAskAgainDelete = false
    private var currentFilter: TodoFilter = TodoFilter.All

    private val TODO_INFO_CHANNEL_ID = "todo_info_channel"
    private val TODO_INFO_CHANNEL_NAME = "Todo Reminder"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
     val DBHelper = DatabaseHelper(this)
    private var isDataAdded = false





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        pref = getSharedPreferences("settings", MODE_PRIVATE)

        checkData()
        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerViews()
        setupFab()
        updateUI()
        setupFilterChips()
        manageDB()
        requestNotificationPermission()

    }

    override fun onResume() {
        super.onResume()
        nm.cancel(1)
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
            activeAdapter = ToDoAdapter(this@MainActivity, this@MainActivity, activeTodos, DBHelper)
            adapter = activeAdapter
        }
        completedTodoRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            completedAdapter = CompTodoAdapter(this@MainActivity, this@MainActivity, completedTodos , DBHelper)
            adapter = completedAdapter
        }
    }

    private fun setupFab() = with(binding) {
        addTodoDialogBtn.setOnClickListener {
            DialogHelper.showAddTodoDialog(this@MainActivity) { todo ->
                addTodo(todo)
            }
        }
    }

    private fun setupFilterChips() {
        binding.filterChips.setOnCheckedChangeListener { _, checkedId ->
            val newFilter = when (checkedId) {
                R.id.chip_active -> TodoFilter.Active
                R.id.chip_completed -> TodoFilter.Completed
                else -> TodoFilter.All
            }
            if (newFilter != currentFilter) {
                saveFilterState(newFilter)
                updateUIVisibility()
            }
        }
    }



    private fun addTodo(todo: ToDoModal) {
        // ✅ save to DB
        DBHelper.addTodo(todo.title)

        // Refresh from DB so we get the auto-generated ID
        val todos = DBHelper.fetchData()
        val newTodo = todos.last() // last inserted

        activeTodos.add(newTodo)
        activeAdapter.notifyItemInserted(activeTodos.size - 1)
        binding.todosRecyclerView.scrollToPosition(activeTodos.size - 1)
        updateUI()
    }


    override fun onTodoItemChecked(item: ToDoModal, position: Int) =
        toggleTodoState(activeTodos, completedTodos, position, activeAdapter, completedAdapter)

    override fun onCompletedItemUnchecked(item: ToDoModal, position: Int) =
        toggleTodoState(completedTodos, activeTodos, position, completedAdapter, activeAdapter)

    private fun toggleTodoState(
        from: MutableList<ToDoModal>,
        to: MutableList<ToDoModal>,
        pos: Int,
        fromAdapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>,
        toAdapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>
    ) {
        from.getOrNull(pos)?.let { item ->
            item.isDone = !item.isDone
            // ✅ update database with new state
            DBHelper.update(item, isChecked = item.isDone)

            from.removeAt(pos).also {
                fromAdapter.notifyItemRemoved(pos)
                to.add(it)
                toAdapter.notifyItemInserted(to.size - 1)
            }
        }
        updateUI()
    }


    override fun onAttemptDelete(callback: (Boolean) -> Unit) {
        if (dontAskAgainDelete) {
            callback(true)
        } else {
            DialogHelper.showConfirmDeleteDialog(this, pref) { confirmed, dontAsk ->
                if (confirmed) {
                    dontAskAgainDelete = dontAsk
                    callback(true)
                } else {
                    callback(false)
                }
            }
        }
    }

    private fun updateUI() {
        updateProgress()
        updateUIVisibility()
    }

    private fun updateUIVisibility() = with(binding) {
        val showActive = activeTodos.isNotEmpty() && (currentFilter is TodoFilter.All || currentFilter is TodoFilter.Active)
        val showCompleted = completedTodos.isNotEmpty() && (currentFilter is TodoFilter.All || currentFilter is TodoFilter.Completed)

        incompleteHeader.visibility = if (showActive) View.VISIBLE else View.GONE
        todosRecyclerView.visibility = if (showActive) View.VISIBLE else View.GONE

        completedHeader.visibility = if (showCompleted) View.VISIBLE else View.GONE
        completedTodoRecycler.visibility = if (showCompleted) View.VISIBLE else View.GONE
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
        todoCount.text = "${total - done} active • $done done"
    }



    override fun onCreateOptionsMenu(menu: Menu?) =
        menuInflater.inflate(R.menu.activity_main_menu, menu).let { true }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_todo_option -> {
                DialogHelper.showAddTodoDialog(this@MainActivity) { todo ->
                    addTodo(todo)
                }
            }
            R.id.exit_option -> finishAffinity()
            R.id.settings_option -> Snackbar.make(binding.root, "This feature isn’t available.", Snackbar.LENGTH_SHORT).show()
            R.id.ask_question_option -> {
                val intent = Intent(Intent.ACTION_SEND)
                    .setType("message/rfc822")
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf("shubhamgupta8609@gmail.com"))
                    .putExtra(Intent.EXTRA_SUBJECT, "Ask Question from Momentum")
                    .putExtra(Intent.EXTRA_TEXT, "Hello developers, I want to ask a question about your app Momentum..")
                try {
                    startActivity(Intent.createChooser(intent, "Send email using..."))
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "No app found to send email.", Snackbar.LENGTH_SHORT).show()
                }
            }
            R.id.contact_us_option -> {
                val intent = Intent(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("smsto:" + Uri.encode("+919082871979")))
                    .putExtra("sms_body", "Hello developer, [Your Query..]")
                try {
                startActivity(intent)
                }catch(e: java.lang.Exception){
                    Snackbar.make(binding.root, "No app found to send SMS.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> Unit
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
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    TODO_INFO_CHANNEL_ID,
                    TODO_INFO_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, TODO_INFO_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Todo Reminder")
            .setContentText("Complete your todos and release dopamine")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun checkData() {
        dontAskAgainDelete = pref.getBoolean("delete_flag", false)
        currentFilter = TodoFilter.fromKey(pref.getString("filter", "all") ?: "all")
        isDataAdded = pref.getBoolean("isDataAdded" , false)
        when (currentFilter) {
            TodoFilter.All -> binding.filterChips.check(R.id.chip_all)
            TodoFilter.Active -> binding.filterChips.check(R.id.chip_active)
            TodoFilter.Completed -> binding.filterChips.check(R.id.chip_completed)
        }


    }

    private fun manageDB(){
        if(!isDataAdded){
    DBHelper.addTodo("Drink water 1")
        DBHelper.addTodo("Drink water 2")
        DBHelper.addTodo("Drink water 3")
        DBHelper.addTodo("Drink water 4")
        DBHelper.addTodo("Drink water 5")
        DBHelper.addTodo("Drink water 6")
        DBHelper.addTodo("Drink water 7")
        DBHelper.addTodo("Drink water 8")
        DBHelper.addTodo("Drink water 9")
        DBHelper.addTodo("Drink water 10")
        DBHelper.addTodo("Drink water 11")
        DBHelper.addTodo("Drink water 12")
        DBHelper.addTodo("Drink water 13")
        DBHelper.addTodo("Drink water 14")
        DBHelper.addTodo("Drink water 15")
        DBHelper.addTodo("Drink water 16")
        DBHelper.addTodo("Drink water 17")
        DBHelper.addTodo("Drink water 18")
        DBHelper.addTodo("Drink water 19")
        DBHelper.addTodo("Drink water 20")
            pref.edit().putBoolean("isDataAdded" , true).apply()
    }
        var restoredTodos = ArrayList<ToDoModal>()
            val todos = DBHelper.fetchData()
            todos.forEach { if(it.isDone) completedTodos.add(it) else activeTodos.add(it) }
        activeAdapter.notifyDataSetChanged()
        completedAdapter.notifyDataSetChanged()
        updateUIVisibility()
    }

    private fun saveFilterState(state: TodoFilter) {
        pref.edit().putString("filter", state.key).apply()
        currentFilter = state
    }


}
