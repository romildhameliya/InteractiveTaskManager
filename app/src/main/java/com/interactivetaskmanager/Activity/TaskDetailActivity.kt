package com.interactivetaskmanager.Activity

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.interactivetaskmanager.Database.Task
import com.interactivetaskmanager.Model.TaskViewModel
import com.interactivetaskmanager.Other.Constant
import com.interactivetaskmanager.R
import com.interactivetaskmanager.databinding.ActivityTaskDetailBinding
import kotlin.math.hypot

class TaskDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityTaskDetailBinding
    private val viewModel: TaskViewModel by viewModels()
    private var task: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        task = Constant.currentTask

        binding.root.post {
            val x = intent.getIntExtra("revealX", binding.root.width / 2)
            val y = intent.getIntExtra("revealY", binding.root.height / 2)
            circularRevealAnimation(x, y)
        }
        setupUI()
        setupListeners()
    }

    private fun circularRevealAnimation(cx: Int, cy: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val finalRadius =
                hypot(binding.root.width.toDouble(), binding.root.height.toDouble()).toFloat()

            // Set initial background color to grey before starting the animation
            val colorFrom = ContextCompat.getColor(this, R.color.grey) // Use your custom grey color
            val colorTo = ContextCompat.getColor(this, R.color.bgColor) // Change this to your desired final color

            binding.root.setBackgroundColor(colorFrom)

            // Create the circular reveal animation
            val anim =
                ViewAnimationUtils.createCircularReveal(binding.root, cx, cy, 0f, finalRadius).apply {
                    duration = 500
                    interpolator = AccelerateDecelerateInterpolator()
                }

            binding.root.visibility = View.VISIBLE
            anim.start()

            // Animate from grey to the final background color
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            colorAnimation.duration = 500 // Same duration as the circular reveal animation
            colorAnimation.addUpdateListener { animator ->
                binding.root.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimation.start()
        } else {
            binding.root.visibility = View.VISIBLE
        }
    }


    private fun setupUI() {
        task?.let {
            binding.tvTaskTitle.text = it.title
            binding.tvTaskDescription.text = it.description ?: "No description"
            binding.tvTaskPriority.text = "Priority: ${it.priority}"
            binding.tvTaskDueDate.text = "Due: ${it.dueDate}"
            binding.tvTaskStatus.text = if (it.isCompleted) "Completed" else "Pending"
            if (it.isCompleted) {
                binding.tvCompleteTask.visibility = View.GONE
                binding.tvTaskStatus.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        binding.root.context, R.color.green
                    )
                )
            }
        }
    }

    private fun setupListeners() {
        binding.tvDeleteTask.setOnClickListener {
            task?.let {
                viewModel.removeTask(it)
                finish()
            }
        }

        binding.tvCompleteTask.setOnClickListener {
            task?.let {
                val updatedTask = it.copy(isCompleted = true)
                viewModel.completeTask(updatedTask)
                binding.tvTaskStatus.text = "Completed"
                binding.tvTaskStatus.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        binding.root.context, R.color.green
                    )
                )
                binding.tvCompleteTask.visibility = View.GONE
            }
        }
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
