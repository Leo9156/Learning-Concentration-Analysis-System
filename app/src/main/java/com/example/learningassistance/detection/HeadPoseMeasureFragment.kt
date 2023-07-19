package com.example.learningassistance.detection

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.learningassistance.R
import com.example.learningassistance.databinding.FragmentCameraPreviewBinding
import com.example.learningassistance.databinding.FragmentHeadPoseMeasureBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.concurrent.Executors

class HeadPoseMeasureFragment : Fragment() {
    private var _binding: FragmentHeadPoseMeasureBinding? = null
    private val binding get() = _binding!!

    // Context
    private lateinit var safeContext: Context

    // Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.safeContext = context
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHeadPoseMeasureBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
            val precautionBottomSheet = HeadPoseMeasurePrecautionBottomSheet()
            precautionBottomSheet.show(
                requireActivity().supportFragmentManager,
                HeadPoseMeasurePrecautionBottomSheet.TAG)
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }


    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(safeContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            }
            else {
                Toast.makeText(safeContext, "Permissions not granted by the user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraFutureProvider = ProcessCameraProvider.getInstance(safeContext)

        cameraFutureProvider.addListener({
            //Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraFutureProvider.get()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Preview usecase
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinderHeadPose.surfaceProvider)
                }

            val imageAnalysisYUV = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val imageAnalysisRGB = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            /*imageAnalysisYUV.setAnalyzer(
                cameraExecutor,
                faceDetectionProcessor
            )

            imageAnalysisRGB.setAnalyzer(
                cameraExecutor,
                objectDetectionProcessor
            )*/

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector/*, imageAnalysisYUV, imageAnalysisRGB*/, preview)
            } catch (exc: Exception) {
                Log.e(HeadPoseMeasureFragment.TAG, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(safeContext))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HeadPoseMeasureFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}