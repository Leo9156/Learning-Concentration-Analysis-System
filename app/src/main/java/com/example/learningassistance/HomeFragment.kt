package com.example.learningassistance

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.learningassistance.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton
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

        // Setting of time picker
        binding.timePicker.maxValue = 120
        binding.timePicker.minValue = 1
        binding.timePicker.value = 60
        binding.timePicker.setOnValueChangedListener { _, _, newVal ->
            binding.learningTime.text = String.format(getString(R.string.main_activity_timer), newVal)
        }

        binding.buttonStart.setOnClickListener {
            showHeadPoseAlertDialog(activity as AppCompatActivity, binding.timePicker.value)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showHeadPoseAlertDialog(activity: AppCompatActivity, learningTime: Int) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("NOTICE")
            .setIcon(R.drawable.ic_notification)
            .setMessage(getString(R.string.start_detection_notification_msg))
            .setPositiveButton(getString(R.string.understand)) { dialog, _ ->
                val intent = Intent(activity, CameraPreviewActivity::class.java)

                val bundle = Bundle()
                bundle.putInt("LEARNING_TIME", learningTime)
                intent.putExtras(bundle)

                startActivity(intent)

                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        private const val TAG = "HomeFragment"
    }

}