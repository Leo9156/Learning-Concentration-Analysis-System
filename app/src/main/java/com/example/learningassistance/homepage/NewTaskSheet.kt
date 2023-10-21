package com.example.learningassistance.homepage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.R
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDatabase
import com.example.learningassistance.databinding.FragmentNewTaskSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NewTaskSheet(private var task: Task?) : BottomSheetDialogFragment() {
    private var _binding: FragmentNewTaskSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNewTaskSheetBinding.inflate(inflater, container, false)
        val view = binding.root

        // Access DAO of the task database
        val application = requireActivity().application
        val dao = TaskDatabase.getInstance(application).taskDao

        // Create view model
        val viewModelFactory = TaskViewModelFactory(dao)
        val taskViewModel = ViewModelProvider(
            requireActivity(),
            viewModelFactory
        ).get(TaskViewModel::class.java)

        // Setting of the hour number pickers
        binding.taskTimeHours.maxValue = 3
        binding.taskTimeHours.minValue = 0

        // Setting of the hour number pickers
        binding.taskTimeMinutes.maxValue = 59
        binding.taskTimeMinutes.minValue = 0
        binding.taskTimeMinutes.setFormatter { value ->
            String.format("%02d", value)
        }

        if (task != null) {
            binding.taskTitle.setText(task!!.taskName)
            binding.taskDiscription.setText(task!!.taskDescription)
            binding.taskTimeHours.value = task!!.taskDurationMin / 60
            binding.taskTimeMinutes.value = task!!.taskDurationMin % 60
            binding.taskSubmitButton.setOnClickListener {
                // Check whether title is empty
                if (binding.taskTitle.text.toString().isEmpty()) {
                    Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    if (binding.taskTimeHours.value == 0 && binding.taskTimeMinutes.value == 0) {
                        Toast.makeText(requireContext(), "Time cannot be 0", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        task!!.taskName = binding.taskTitle.text.toString()
                        task!!.taskDescription = binding.taskDiscription.text.toString()
                        task!!.taskDurationMin = (binding.taskTimeHours.value * 60 + binding.taskTimeMinutes.value)
                        // Reset the task original state
                        task!!.taskTimeLeftMs = (task!!.taskDurationMin * 60000).toLong()
                        task!!.taskCompletePercentage = 0
                        task!!.electronicDevicesTimeMs = 0L
                        task!!.lookAroundTimeMs = 0L
                        task!!.fatigueTimeMs = 0L
                        task!!.noFaceTimeMs = 0L
                        taskViewModel.updateTask(task!!)
                        dismiss()
                    }
                }
            }
        } else {
            // Set the onClickListener of the submit button
            binding.taskSubmitButton.setOnClickListener {
                // Check whether title is empty
                if (binding.taskTitle.text.toString().isEmpty()) {
                    Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    if (binding.taskTimeHours.value == 0 && binding.taskTimeMinutes.value == 0) {
                        Toast.makeText(requireContext(), "Time cannot be 0", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        taskViewModel.newTaskName = binding.taskTitle.text.toString()
                        taskViewModel.newTaskDescription = binding.taskDiscription.text.toString()
                        taskViewModel.newTaskDuration = (binding.taskTimeHours.value * 60 + binding.taskTimeMinutes.value)
                        taskViewModel.addTask()
                        dismiss()
                    }
                }
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val TAG = "NewTaskSheetFragment"
    }
}