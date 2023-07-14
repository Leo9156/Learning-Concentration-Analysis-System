package com.example.learningassistance.homepage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import com.example.learningassistance.R
import com.example.learningassistance.databinding.FragmentNewTaskSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NewTaskSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentNewTaskSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNewTaskSheetBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting of the hour number pickers
        binding.taskTimeHours.maxValue = 3
        binding.taskTimeHours.minValue = 0
        binding.taskTimeHours.value = 1
        /*binding.taskTimeHours.setFormatter { value ->
            String.format("%02d", value)
        }*/

        // Setting of the hour number pickers
        binding.taskTimeMinutes.maxValue = 59
        binding.taskTimeMinutes.minValue = 0
        binding.taskTimeMinutes.value = 0
        binding.taskTimeMinutes.setFormatter { value ->
            String.format("%02d", value)
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