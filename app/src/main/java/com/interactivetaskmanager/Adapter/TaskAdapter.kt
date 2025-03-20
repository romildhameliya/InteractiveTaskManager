package com.interactivetaskmanager.Adapter

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import com.interactivetaskmanager.Activity.TaskDetailActivity
import com.interactivetaskmanager.Database.Task
import com.interactivetaskmanager.Model.TaskViewModel
import com.interactivetaskmanager.Other.Constant
import com.interactivetaskmanager.R
import com.interactivetaskmanager.databinding.ItemTaskBinding

class TaskAdapter(
    private val context: Context,
    var tasks: MutableList<Task>,
    val onDeleteTask: (Task) -> Unit,
    val onCompleteTask: (Task) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isLoading = true

    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_SHIMMER = 0
        private const val SHIMMER_ITEM_COUNT = 5
    }

    override fun getItemViewType(position: Int): Int =
        if (isLoading) VIEW_TYPE_SHIMMER else VIEW_TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SHIMMER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_task_shimmer, parent, false)
            ShimmerViewHolder(view)
        } else {
            val binding =
                ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            TaskViewHolder(binding)
        }
    }

    override fun getItemCount(): Int = if (isLoading) SHIMMER_ITEM_COUNT else tasks.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TaskViewHolder) {
            val task = tasks[position]
            holder.bind(task)
            holder.itemView.setOnClickListener { view ->
                val taskDetailIntent = Intent(context, TaskDetailActivity::class.java)

                val location = IntArray(2)
                view.getLocationOnScreen(location)
                taskDetailIntent.putExtra("revealX", location[0] + view.width / 2)
                taskDetailIntent.putExtra("revealY", location[1] + view.height / 2)

                Constant.currentTask = task
                context.startActivity(taskDetailIntent)
            }
        } else if (holder is ShimmerViewHolder) {
            holder.shimmerFrameLayout.startShimmer()
        }
    }

    fun updateTasks(newTasks: MutableList<Task>) {
        isLoading = false
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskDescription.text = task.description ?: "No description"
            binding.tvTaskPriority.text = "Priority: ${task.priority}"
            binding.tvTaskDueDate.text = "Due: ${task.dueDate}"

            binding.tvTaskStatus.text = if (task.isCompleted) "Completed" else "Pending"
            binding.tvTaskStatus.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    binding.root.context,
                    if (task.isCompleted) R.color.green else R.color.red
                )
            )
        }
    }

    class ShimmerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shimmerFrameLayout: ShimmerFrameLayout = itemView as ShimmerFrameLayout
    }
}

fun attachSwipeGesture(recyclerView: RecyclerView, adapter: TaskAdapter, viewModel: TaskViewModel) {
    val itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val task = adapter.tasks[position]

                if (direction == ItemTouchHelper.LEFT) {
                    adapter.onDeleteTask(task)
                    Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG).show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    adapter.onCompleteTask(task)
                    val updatedTask = task.copy(isCompleted = true)
                    adapter.tasks[position] = updatedTask
                    adapter.notifyItemChanged(position)
                    Snackbar.make(recyclerView, "Task marked as completed", Snackbar.LENGTH_LONG).show()
                }

                // Reset Sorting & Filtering after task modification
                recyclerView.postDelayed({
                    viewModel.refreshTasks() // Call a function to refresh tasks
                }, 300) // Small delay ensures smooth UI update
            }
        }
    ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
}
