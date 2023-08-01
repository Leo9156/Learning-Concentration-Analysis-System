package com.example.learningassistance.historypage

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.learningassistance.R
import com.example.learningassistance.database.TaskDatabase
import com.example.learningassistance.databinding.FragmentAnalysisChartBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class AnalysisChartFragment : Fragment() {
    private var _binding: FragmentAnalysisChartBinding? = null
    private val binding get() = _binding!!
    private lateinit var context: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAnalysisChartBinding.inflate(inflater, container, false)
        val view = binding.root

        // database
        val application = requireActivity().application
        val dao = TaskDatabase.getInstance(application).taskDao

        // View model
        val taskId = AnalysisChartFragmentArgs.fromBundle(requireArguments()).taskId
        val viewModelFactory = AnalysisChartViewModelFactory(dao, taskId)
        val viewModel = ViewModelProvider(
            requireActivity(),
            viewModelFactory
        ).get(AnalysisChartViewModel::class.java)

        viewModel.updateId(taskId)
        viewModel.setTask()

        viewModel.task.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Log.v(TAG, "id: ${viewModel.id}")
                viewModel.calculatePercent()
                binding.attentionScore.text = String.format(getString(R.string.attention_score_actual), viewModel.attentionPercent)
                initPieChart()
                showPieChart(viewModel)
                viewModel.task.value = null
            }
        })

        binding.homeBtn.setOnClickListener {
            this.findNavController().navigate(R.id.action_analysisChartFragment_to_historyFragment)
        }

        return view
    }

    private fun initPieChart() {
        binding.analysisChart.setUsePercentValues(true)
        binding.analysisChart.description.isEnabled = false
        binding.analysisChart.isRotationEnabled = true
        binding.analysisChart.dragDecelerationFrictionCoef = 0.95f  // Adding friction coef when rotating the chart
        binding.analysisChart.isHighlightPerTapEnabled = true
        binding.analysisChart.animateY(1400, Easing.EaseInOutQuad)
        binding.analysisChart.setExtraOffsets(5f, 5f, 5f, 5f)
        binding.analysisChart.minAngleForSlices = 20f
    }

    private fun showPieChart(viewModel: AnalysisChartViewModel) {
        // Elements shown in the chart
        val pieEntries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        if (viewModel.noFacePercent > 0f) {
            pieEntries.add(PieEntry(viewModel.noFacePercent, getString(R.string.leave)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_red))
        }
        if (viewModel.drowsinessPercent > 0f) {
            pieEntries.add(PieEntry(viewModel.drowsinessPercent, getString(R.string.fatigue)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_sec_red))
        }
        if (viewModel.lookAroundPercent > 0f) {
            pieEntries.add(PieEntry(viewModel.lookAroundPercent, getString(R.string.look_around)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_orange))
        }
        if (viewModel.electDevPercent > 0f) {
            pieEntries.add(PieEntry(viewModel.electDevPercent, getString(R.string.elect_dev)))
            colors.add(ContextCompat.getColor(context, R.color.pie_color_yellow))
        }
        if (viewModel.attentionPercent > 0f) {
            pieEntries.add(PieEntry(viewModel.attentionPercent, getString(R.string.attention)))
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
        pieData.setValueFormatter(PercentFormatter(binding.analysisChart))
        binding.analysisChart.data = pieData

        // Draw the chart
        binding.analysisChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG = "AnalysisChartFragment"
    }
}