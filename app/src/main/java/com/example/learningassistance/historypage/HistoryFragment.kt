
package com.example.learningassistance.historypage

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.learningassistance.database.TaskDatabase
import com.example.learningassistance.databinding.FragmentHistoryBinding
import java.time.LocalDate

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root

        // Accessing dao of the database
        val application = requireActivity().application
        val dao = TaskDatabase.getInstance(application).taskDao

        // Create the view model
        val viewModelFactory = HistoryViewModelFactory(dao)
        val viewModel = ViewModelProvider(
            requireActivity(),
            viewModelFactory
        ).get(HistoryViewModel::class.java)

        // Calendar view
        binding.calendar.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.setHistTasksDate(selectedDate)
        }

        viewModel.setHistTasksDate(LocalDate.now().toString())

        // recycler view
        val adapter = HistoryTaskItemAdapter { task ->
            Log.v(TAG, "${task.taskId}")
            val action =
                HistoryFragmentDirections.actionHistoryFragmentToAnalysisChartFragment(task.taskId)
            this.findNavController().navigate(action)
        }
        binding.calendarTask.adapter = adapter
        viewModel.histTasks.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.data = it
                viewModel.histTasks.value = null
            }
        })

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG = "HistoryFragment"
    }
}