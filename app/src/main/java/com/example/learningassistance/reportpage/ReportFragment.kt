package com.example.learningassistance.reportpage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.R
import com.example.learningassistance.database.TaskDatabase
import com.example.learningassistance.databinding.FragmentReportBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ReportViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        val view = binding.root

        // Database
        val application = requireActivity().application
        val dao = TaskDatabase.getInstance(application).taskDao

        // View model
        val viewModelFactory = ReportViewModelFactory(dao)
        viewModel = ViewModelProvider(
            requireActivity(),
            viewModelFactory
        ).get(ReportViewModel::class.java)

        // Retrieve the data from the database
        viewModel.setTasks()

        // UI handeling
        viewModel.tasks.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                viewModel.calculateAvg()

                // Average attention score card view
                binding.barScore.progress = viewModel.avgAttentionScore.toInt()
                binding.tvScore.text = viewModel.avgAttentionScore.toString()
                binding.tvScoreAndTotal.text = String.format(getString(R.string.attention_score_total), viewModel.avgAttentionScore)

                // Average Learning time card view
                binding.dayLearningTime.text = String.format(getString(R.string.learning_time_per_day), viewModel.avgLearningTimeMin.toInt())

                // Evaluation card
                when {
                    viewModel.avgAttentionScore >= 90f -> {
                        binding.tvEvaluation.text = getString(R.string.excellent)
                        binding.evaluationIcon.setImageResource(R.drawable.ic_congratulation)
                    }
                    viewModel.avgAttentionScore >= 80f -> {
                        binding.tvEvaluation.text = getString(R.string.great)
                        binding.evaluationIcon.setImageResource(R.drawable.ic_great)
                    }
                    viewModel.avgAttentionScore >= 60f -> {
                        binding.tvEvaluation.text = getString(R.string.so_so)
                        binding.evaluationIcon.setImageResource(R.drawable.ic_soso)
                    }
                    viewModel.avgAttentionScore >= 40f -> {
                        binding.tvEvaluation.text = getString(R.string.should_be_improved)
                        binding.evaluationIcon.setImageResource(R.drawable.ic_improved)
                    }
                    else -> {
                        binding.tvEvaluation.text = getString(R.string.bad)
                        binding.evaluationIcon.setImageResource(R.drawable.ic_sad)
                    }
                }

                // Draw line chart
                drawChart()
            }
        })

        return view
    }

    private fun initChart() {
        // Disable grid
        binding.reportChart.setDrawGridBackground(false)

        // Disable description
        val description = Description()
        description.isEnabled = false
        binding.reportChart.description = description

        // Animation
        binding.reportChart.animateY(1000, Easing.Linear)
        binding.reportChart.animateX(1000)

        // xAsis
        val xAsis = binding.reportChart.xAxis
        xAsis.position = XAxis.XAxisPosition.BOTTOM
        xAsis.granularity = 1f
        val xLabels = arrayOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        xAsis.valueFormatter = IndexAxisValueFormatter(xLabels)
        xAsis.labelCount = 7
        xAsis.setDrawGridLines(false)

        // Left yAxis
        binding.reportChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 100f
            labelCount = 5
            setDrawTopYLabelEntry(true)
        }

        // Right yAxis
        binding.reportChart.axisRight.isEnabled = false
    }

    private fun drawChart() {
        initChart()

        val barEntry = ArrayList<BarEntry>()
        //if (viewModel.avgAttentionScoreMon != 0f) {
            barEntry.add(BarEntry(1f, viewModel.avgAttentionScoreMon))
        //}
        //if (viewModel.avgAttentionScoreTue != 0f) {
            barEntry.add(BarEntry(2f, viewModel.avgAttentionScoreTue))
        //}
        //if (viewModel.avgAttentionScoreWed != 0f) {
            barEntry.add(BarEntry(3f, viewModel.avgAttentionScoreWed))
        //}
        //if (viewModel.avgAttentionScoreThu != 0f) {
            barEntry.add(BarEntry(4f, viewModel.avgAttentionScoreThu))
        //}
        //if (viewModel.avgAttentionScoreFri != 0f) {
            barEntry.add(BarEntry(5f, viewModel.avgAttentionScoreFri))
        //}
        //if (viewModel.avgAttentionScoreSat != 0f) {
            barEntry.add(BarEntry(6f, viewModel.avgAttentionScoreSat))
        //}
        //if (viewModel.avgAttentionScoreSun != 0f) {
            barEntry.add(BarEntry(7f, viewModel.avgAttentionScoreSun))
        //}

        val dataSet = BarDataSet(barEntry, "Average attention score per day")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary)
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        binding.reportChart.data = barData

        binding.reportChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.reset()
        _binding = null
    }

}