package com.example.learningassistance.detection.result

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.MainActivity
import com.example.learningassistance.R
import com.example.learningassistance.database.TaskDatabase
import com.example.learningassistance.databinding.FragmentAnalysisResultsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class AnalysisResultsFragment : Fragment() {
    // Context
    private lateinit var context: Context
    // View binding
    private var _binding: FragmentAnalysisResultsBinding? = null
    private val binding get() = _binding!!

    // View model
    private var viewModel: AnalysisResultsViewModel? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAnalysisResultsBinding.inflate(inflater, container, false)
        val view = binding.root

        // database
        val application = requireActivity().application
        val dao = TaskDatabase.getInstance(application).taskDao

        // Initialize view model
        if (viewModel == null) {
            val taskId = requireActivity().intent.extras!!.getLong("taskId")
            val viewModelFactory = AnalysisResultsViewModelFactory(dao, taskId)
            viewModel = ViewModelProvider(
                requireActivity(),
                viewModelFactory
            ).get(AnalysisResultsViewModel::class.java)
        }

        viewModel!!.task.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                viewModel!!.calculatePercent()
                initPieChart()
                showPieChart()
            }
        })

        binding.resultNavigateButton.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun initPieChart() {
        binding.resultChart.setUsePercentValues(true)
        binding.resultChart.description.isEnabled = false
        binding.resultChart.isRotationEnabled = true
        binding.resultChart.dragDecelerationFrictionCoef = 0.95f  // Adding friction coef when rotating the chart
        binding.resultChart.isHighlightPerTapEnabled = true
        binding.resultChart.animateY(1400, Easing.EaseInOutQuad)
        binding.resultChart.setExtraOffsets(5f, 5f, 5f, 5f)
        binding.resultChart.minAngleForSlices = 20f
    }

    private fun showPieChart() {
        // Elements shown in the chart
        val pieEntries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        if (viewModel!!.noFacePercent > 0f) {
            pieEntries.add(PieEntry(viewModel!!.noFacePercent, getString(R.string.leave)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_red))
        }
        if (viewModel!!.drowsinessPercent > 0f) {
            pieEntries.add(PieEntry(viewModel!!.drowsinessPercent, getString(R.string.fatigue)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_sec_red))
        }
        if (viewModel!!.lookAroundPercent > 0f) {
            pieEntries.add(PieEntry(viewModel!!.lookAroundPercent, getString(R.string.look_around)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_orange))
        }
        if (viewModel!!.electDevPercent > 0f) {
            pieEntries.add(PieEntry(viewModel!!.electDevPercent, getString(R.string.elect_dev)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_yellow))
        }
        if (viewModel!!.attentionPercent > 0f) {
            pieEntries.add(PieEntry(viewModel!!.attentionPercent, getString(R.string.attention)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_green))
        }

        // Create dataset
        val pieDataSet = PieDataSet(pieEntries, getString(R.string.behavior))
        pieDataSet.colors = colors

        // Create data and add it into the pieChart
        val pieData = PieData(pieDataSet)
        pieData.setDrawValues(true)
        pieData.setValueTextSize(15f)
        pieData.setValueTextColor(Color.WHITE)
        pieData.setValueFormatter(PercentFormatter(binding.resultChart))
        binding.resultChart.data = pieData

        // Draw the chart
        binding.resultChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG = "AnalysisResultsFragment"
    }
}