package com.interactivetaskmanager.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.interactivetaskmanager.Adapter.TaskAdapter
import com.interactivetaskmanager.Adapter.attachSwipeGesture
import com.interactivetaskmanager.Model.TaskViewModel
import com.interactivetaskmanager.Other.Constant
import com.interactivetaskmanager.R
import com.interactivetaskmanager.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySavedTheme()

        setupRecyclerView()
        setupListeners()
        observeTasks()
    }


    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(this, mutableListOf(), { task ->
            viewModel.removeTask(task)
            binding.spinnerSort.text = "Sort: by"
            binding.spinnerFilter.text = "Filter: by"
        }, { task ->
            viewModel.completeTask(task)
            binding.spinnerSort.text = "Sort: by"
            binding.spinnerFilter.text = "Filter: by"
        })
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }
        attachSwipeGesture(binding.recyclerView, taskAdapter,viewModel)
        attachDragAndDrop(binding.recyclerView)
    }

    private fun setupListeners() {
        binding.fabAddTask.setOnClickListener {  view ->
            animateFab()

            val taskDetailIntent = Intent(this, TaskCreationActivity::class.java)

            val location = IntArray(2)
            view.getLocationOnScreen(location)
            taskDetailIntent.putExtra("revealX", location[0] + view.width / 2)
            taskDetailIntent.putExtra("revealY", location[1] + view.height / 2)

            startActivity(Intent(this, TaskCreationActivity::class.java))
        }
        binding.ivSetting.setOnClickListener {
            animateFab()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        val sortingOptions = listOf("Priority", "Due Date", "Alphabetically")
        val filteringOptions = listOf("All", "Completed", "Pending")

        binding.spinnerSort.setOnClickListener { view ->
            showPopupMenu(true,view, sortingOptions) { selectedOption ->
                viewModel.sortTasks(selectedOption)
                binding.spinnerSort.text = "Sort: $selectedOption"
                binding.ivDropdownArrowSort.setImageResource(R.drawable.ic_arrow_down) // Reset to down arrow
            }
        }
        binding.spinnerFilter.setOnClickListener { view ->
            showPopupMenu(false,view, filteringOptions) { selectedOption ->
                viewModel.filterTasks(selectedOption)
                binding.spinnerFilter.text = "Filter: $selectedOption"
                binding.ivDropdownArrowFilter.setImageResource(R.drawable.ic_arrow_down) // Reset to down arrow
            }
        }
    }

    private fun applySavedTheme() {
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        val isDarkMode = sharedPreferences.getBoolean("DarkMode", false)
        val primaryColor = sharedPreferences.getInt("PrimaryColor", getColor(R.color.buttonColor))

        // Apply dark mode
        val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
        else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)

        // Apply saved primary color dynamically
        window.statusBarColor = primaryColor
    }


    // Function to Show Pop-Up Menu

    private fun showPopupMenu(isSort : Boolean,view: View, options: List<String>, onItemSelected: (String) -> Unit) {
        val popupMenu = PopupMenu(this, view)
        options.forEachIndexed { index, option ->
            popupMenu.menu.add(0, index, index, option)
        }

        if (isSort)
            binding.ivDropdownArrowSort.setImageResource(R.drawable.ic_arrow_up)
        else
            binding.ivDropdownArrowFilter.setImageResource(R.drawable.ic_arrow_up)

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            val selectedOption = options[menuItem.itemId]
            onItemSelected(selectedOption)
            true
        }

        popupMenu.setOnDismissListener {
            if (isSort)
                binding.ivDropdownArrowSort.setImageResource(R.drawable.ic_arrow_down)
            else
                binding.ivDropdownArrowFilter.setImageResource(R.drawable.ic_arrow_down)
        }

        // Ensure Popup Width Matches Button Width
        popupMenu.show()
        try {
            val popupField = PopupMenu::class.java.getDeclaredField("mPopup")
            popupField.isAccessible = true
            val popupWindow = popupField.get(popupMenu) as PopupWindow
            popupWindow.width = view.width // Set width same as button
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeTasks() {
        // Show loading before collecting data
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE

        lifecycleScope.launch {
            // Wait until the first data is available
            val initialTasks = viewModel.filteredTasks.first()
            taskAdapter.updateTasks(initialTasks.toMutableList())
            updateEmptyState(initialTasks.isEmpty())

            // Continue observing changes
            viewModel.filteredTasks.collect { taskList ->
                taskAdapter.updateTasks(taskList.toMutableList())
                updateEmptyState(taskList.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun animateFab() {
        binding.fabAddTask.animate().apply {
            scaleX(1.2f)
            scaleY(1.2f)
            duration = 150
        }.withEndAction {
            binding.fabAddTask.animate().scaleX(1f).scaleY(1f).duration = 150
        }
    }

    private fun attachDragAndDrop(recyclerView: RecyclerView) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                taskAdapter.tasks.apply {
                    val movedItem = removeAt(fromPosition)
                    add(toPosition, movedItem)
                }
                taskAdapter.notifyItemMoved(fromPosition, toPosition)

                val vibrator = getSystemService<Vibrator>()
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        50, VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

            override fun isLongPressDragEnabled(): Boolean = true
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }
}
