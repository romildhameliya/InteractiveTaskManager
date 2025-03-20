package com.interactivetaskmanager.Model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.interactivetaskmanager.Database.Task
import com.interactivetaskmanager.Database.TaskDatabase
import com.interactivetaskmanager.Database.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val _filteredTasks = MutableStateFlow<List<Task>>(emptyList())
    val filteredTasks: StateFlow<List<Task>> = _filteredTasks

    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks

    init {
        val taskDao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        fetchTasks()
    }

    private fun fetchTasks() {
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                _allTasks.value = tasks
                _filteredTasks.value = tasks
            }
        }
    }

    fun insert(task: Task) = viewModelScope.launch {
        repository.insert(task)
    }

    fun removeTask(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    fun completeTask(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(isCompleted = true)
        repository.update(updatedTask)
    }

    fun sortTasks(option: String) {
        _filteredTasks.value = when (option) {
            "Priority" -> _allTasks.value.sortedBy { it.priority }
            "Due Date" -> _allTasks.value.sortedBy { it.dueDate }
            "Alphabetically" -> _allTasks.value.sortedBy { it.title }
            else -> _allTasks.value
        }
    }

    fun filterTasks(status: String) {
        _filteredTasks.value = when (status) {
            "Completed" -> _allTasks.value.filter { it.isCompleted }
            "Pending" -> _allTasks.value.filter { !it.isCompleted }
            else -> _allTasks.value
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                _allTasks.value = tasks
                _filteredTasks.value = _allTasks.value // Reset filter and sorting
            }
        }
    }

}

class TaskViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}