package com.example.learningassistance.homepage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.learningassistance.CameraPreviewActivity
import com.example.learningassistance.DetectionActivity
import com.example.learningassistance.R
import com.example.learningassistance.database.TaskDao
import com.example.learningassistance.database.TaskDatabase
import com.example.learningassistance.databinding.FragmentHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        // Call the task bottom sheet
        binding.addTaskButton.setOnClickListener {
            NewTaskSheet(null).show(requireActivity().supportFragmentManager, NewTaskSheet.TAG)
        }

        // Initialize the dao interface of the task database
        val application = requireActivity().application
        val dao = TaskDatabase.getInstance(application).taskDao

        // Initialize the view model
        val taskViewModelFactory = TaskViewModelFactory(dao)
        val taskViewModel = ViewModelProvider(
            requireActivity(),
            taskViewModelFactory
        ).get(TaskViewModel::class.java)

        // Initialize the adapter and set the it to the recycler view
        val adapter = TaskItemAdapter(
            taskViewModel,
            { task -> NewTaskSheet(task).show(requireActivity().supportFragmentManager, NewTaskSheet.TAG) },
            { task ->
                /*val action = HomeFragmentDirections.actionHomeFragmentToCameraPreviewActivity(task.taskDurationMin)
                this.findNavController().navigate(action)*/
                //this.findNavController().navigate(R.id.action_homeFragment_to_cameraPreviewFragment)
                val intent = Intent(requireContext(), DetectionActivity::class.java)
                /*val bundle = Bundle()
                bundle.putInt("LEARNING_TIME", task.taskDurationMin)
                intent.putExtras(bundle)*/
                startActivity(intent)
        })
        binding.taskRecyclerView.adapter = adapter

        // Listen to the task in the database. If changed, than modify the recycler view
        taskViewModel.tasks.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.data = it
                taskViewModel.totalTasksCount.value = adapter.data.size
            }
        })

        // Observe whether the total tasks count in the task view model has changed
        taskViewModel.totalTasksCount.observe(viewLifecycleOwner, Observer {
            binding.taskTotal.text = String.format(
                getString(R.string.home_page_task_summary_msg),
                taskViewModel.totalTasksCount.value!!)
        })

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HomeFragment"
    }

}