package com.interactivetaskmanager.Activity

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.interactivetaskmanager.Database.Task
import com.interactivetaskmanager.Model.TaskViewModel
import com.interactivetaskmanager.R
import com.interactivetaskmanager.databinding.ActivityTaskCreationBinding
import java.util.*
import kotlin.math.hypot

class TaskCreationActivity : BaseActivity() {
    private lateinit var binding: ActivityTaskCreationBinding
    private val viewModel: TaskViewModel by viewModels()
    val priorities = listOf("Low", "Medium", "High")
    var selectedTaskPriority: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvHigh.isSelected = false
        binding.tvMedium.isSelected = false
        binding.tvLow.isSelected = true
        selectedTaskPriority = 0

        binding.root.post {
            val x = intent.getIntExtra("revealX", binding.root.width / 2)
            val y = intent.getIntExtra("revealY", binding.root.height / 2)
            circularRevealAnimation(x, y)
        }


        setOnClickListners()
    }


    private fun setOnClickListners() {
        binding.llSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    binding.tvDueDate.text = selectedDate
                },
                year, month, day
            )
            datePicker.show()

        }
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.tvLow.setOnClickListener {
            binding.tvHigh.isSelected = false
            binding.tvMedium.isSelected = false
            binding.tvLow.isSelected = true
            selectedTaskPriority = 0
        }
        binding.tvMedium.setOnClickListener {
            binding.tvHigh.isSelected = false
            binding.tvLow.isSelected = false
            binding.tvMedium.isSelected = true
            selectedTaskPriority = 1
        }
        binding.tvHigh.setOnClickListener {
            binding.tvLow.isSelected = false
            binding.tvMedium.isSelected = false
            binding.tvHigh.isSelected = true
            selectedTaskPriority = 2
        }
        binding.tvSaveTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val description = binding.etTaskDescription.text.toString().trim()
            val priority = priorities[selectedTaskPriority].toString().trim()
            val dueDate = binding.tvDueDate.text.toString().trim()

            if (validateInputs(title, description, priority, dueDate)) {
                val newTask = Task(title = title, description = description, priority = priority!!, dueDate = dueDate)
                viewModel.insert(newTask)
                finish()
            }
            animateTv()
        }

    }

    private fun circularRevealAnimation(cx: Int, cy: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val finalRadius =
                hypot(binding.root.width.toDouble(), binding.root.height.toDouble()).toFloat()

            // Set initial background color to grey before starting the animation
            val colorFrom = ContextCompat.getColor(this, R.color.grey) // Use your custom grey color
            val colorTo = ContextCompat.getColor(this, R.color.white) // Change this to your desired final color

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

    private fun animateTv() {
        binding.tvSaveTask.animate().apply {
            scaleX(1.2f)
            scaleY(1.2f)
            duration = 150
        }.withEndAction {
            binding.tvSaveTask.animate().scaleX(1f).scaleY(1f).duration = 150
        }
    }

    private fun validateInputs(title: String, description: String, priority: String?, dueDate: String): Boolean {
        var isValid = true

        if (title.isBlank()) {
            binding.etTaskTitle.error = "Title is required"
            isValid = false
        }

        if (priority.isNullOrBlank()) {
            Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (dueDate.isBlank() || dueDate == "No date selected") {
            Toast.makeText(this, "Please select a due date", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }
}