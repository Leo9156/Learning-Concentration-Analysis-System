package com.example.learningassistance.detection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.learningassistance.databinding.FragmentHeadPoseMeasurePrecautionBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class HeadPoseMeasurePrecautionBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentHeadPoseMeasurePrecautionBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeadPoseMeasurePrecautionBottomSheetBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.precautionConfirmButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireDialog() as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val TAG = "HeadPoseMeasurePrecautionBottomSheetFragment"
    }
}