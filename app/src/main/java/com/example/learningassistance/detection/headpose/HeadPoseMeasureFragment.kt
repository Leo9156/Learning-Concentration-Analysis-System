package com.example.learningassistance.detection.headpose

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.learningassistance.R
import com.example.learningassistance.databinding.FragmentHeadPoseMeasureBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HeadPoseMeasureFragment : Fragment() {
    // view binding
    private var _binding: FragmentHeadPoseMeasureBinding? = null
    private val binding get() = _binding!!

    // view model
    private var headPoseViewModel: HeadPoseMeasureViewModel? = null

    // Context
    private lateinit var safeContext: Context

    // Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService

    // Face detection processor
    private lateinit var headPoseFaceDetectionProcessor: HeadPoseFaceDetectionProcessor

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

        // Initialize the face detector
        headPoseFaceDetectionProcessor = HeadPoseFaceDetectionProcessor()
        headPoseFaceDetectionProcessor.start()

        val headPoseMeasureViewModelFactory = HeadPoseMeasureViewModelFactory(headPoseFaceDetectionProcessor)

        if (headPoseViewModel == null) {
            // Log.v(TAG, "Create a new view model")
            headPoseViewModel = ViewModelProvider(
                requireActivity(),
                headPoseMeasureViewModelFactory
            ).get(HeadPoseMeasureViewModel::class.java)
        }

        // Need to update the processor because when navigating from concentration fragment
        headPoseViewModel!!.updateProcessor(headPoseFaceDetectionProcessor)

        if (allPermissionsGranted()) {
            // Start the camera
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Show the precaution sheet
        val precautionBottomSheet = HeadPoseMeasurePrecautionBottomSheet()
        precautionBottomSheet.show(
            requireActivity().supportFragmentManager,
            HeadPoseMeasurePrecautionBottomSheet.TAG
        )

        // UI handle
        // Face detection textview in the info card
        headPoseFaceDetectionProcessor.isFaceDetected.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.headPoseFaceDetectionInfo.text = getString(R.string.head_pose_analysis_title_detected)
                binding.headPoseFaceDetectionInfo.setTextColor(ContextCompat.getColor(safeContext, R.color.green))
                binding.headPoseFaceDetectionIcon.setImageResource(R.drawable.ic_check2)
            } else {
                binding.headPoseFaceDetectionInfo.text = getString(R.string.head_pose_analysis_title_not_detected)
                binding.headPoseFaceDetectionInfo.setTextColor(ContextCompat.getColor(safeContext, R.color.red))
                binding.headPoseFaceDetectionIcon.setImageResource(R.drawable.ic_error)
            }
        })

        // Rotation degrees textview in the info card
        headPoseFaceDetectionProcessor.rotX.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding.headPoseRotateDegreeX.visibility = View.VISIBLE
                binding.headPoseRotateIconX.visibility = View.VISIBLE
                binding.headPoseRotateDegreeX.text = String.format(getString(R.string.head_pose_card_eulerx), it)
            } else {
                binding.headPoseRotateDegreeX.visibility = View.INVISIBLE
                binding.headPoseRotateIconX.visibility = View.INVISIBLE
            }
        })

        headPoseFaceDetectionProcessor.rotY.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding.headPoseRotateDegreeY.visibility = View.VISIBLE
                binding.headPoseRotateIconY.visibility = View.VISIBLE
                binding.headPoseRotateDegreeY.text = String.format(getString(R.string.head_pose_card_eulery), it)
            } else {
                binding.headPoseRotateDegreeY.visibility = View.INVISIBLE
                binding.headPoseRotateIconY.visibility = View.INVISIBLE
            }
        })

        // Start detection button
        binding.headPoseStartButton.setOnClickListener {
            if (headPoseViewModel!!.canStartAnalysis()) {
                headPoseViewModel!!.startPrepareTimer.value = true  // Should start the prepare timer
            } else {
                Snackbar.make(
                    binding.headPoseStartButton,
                    getString(R.string.head_pose_analysis_cannot_start),
                    Snackbar.LENGTH_SHORT).show()
            }
        }

        // Prepare Timer
        headPoseViewModel!!.startPrepareTimer.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.headPoseStartButton.visibility = View.INVISIBLE
                headPoseViewModel!!.startPrepareTimer()
            } else {
                binding.headPoseStartButton.visibility = View.VISIBLE
                binding.headPoseStartTimer.visibility = View.INVISIBLE
            }
        })
        headPoseViewModel!!.prepareTimerLeftCount.observe(viewLifecycleOwner, Observer {
            if (it == 0) {
                binding.headPoseStartTimer.visibility = View.INVISIBLE
            } else {
                binding.headPoseStartTimer.visibility = View.VISIBLE
                binding.headPoseStartTimer.text = it.toString()
                val mediaPlayer = MediaPlayer.create(safeContext, R.raw.beep_sound)
                mediaPlayer.setOnCompletionListener {
                    it.release()
                }
                mediaPlayer.start()
            }
        })

        // Analysis Timer
        headPoseViewModel!!.isAnalysisStarting.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.headPoseAnalysisProgress.visibility = View.VISIBLE
                binding.headPoseStartButton.visibility = View.INVISIBLE
            } else {
                binding.headPoseAnalysisProgress.visibility = View.INVISIBLE
            }
        })
        headPoseViewModel!!.analysisProgress.observe(viewLifecycleOwner, Observer {
            binding.headPoseAnalysisProgress.progress = it
        })
        headPoseViewModel!!.isAnalysisFinished.observe(viewLifecycleOwner, Observer {
            if (it) {
                // Play the sound
                val mediaPlayer = MediaPlayer.create(safeContext, R.raw.head_pose_complete)
                mediaPlayer.setOnCompletionListener {
                    it.release()
                }
                mediaPlayer.start()

                // Reset the state of the view model
                headPoseViewModel!!.reset()

                // Launch the concentration analysis fragment
                val action =
                    HeadPoseMeasureFragmentDirections.actionHeadPoseMeasureFragmentToCameraPreviewFragment(
                        headPoseFaceDetectionProcessor.headEulerXOffset,
                        headPoseFaceDetectionProcessor.headEulerYOffset
                    )
                Log.v(TAG, "x: ${headPoseFaceDetectionProcessor.headEulerXOffset}")
                Log.v(TAG, "y: ${headPoseFaceDetectionProcessor.headEulerYOffset}")
                this.findNavController().navigate(action)
            }
        })

        // Restart handling
        headPoseFaceDetectionProcessor.hasToRestart.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.headPoseStartButton.visibility = View.VISIBLE
                headPoseViewModel!!.restart()
                MaterialAlertDialogBuilder(safeContext)
                    .setTitle(getString(R.string.head_pose_analysis_title_not_detected))
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(getString(R.string.head_pose_analysis_restart_msg))
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        })
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

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinderHeadPose.surfaceProvider)
                }

            // Image Analysis use case
            val imageAnalysisYUV = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysisYUV.setAnalyzer(
                cameraExecutor,
                headPoseFaceDetectionProcessor
            )


            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisYUV, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Camera use case binding failed")
            }
        }, ContextCompat.getMainExecutor(safeContext))
    }

    override fun onPause() {
        super.onPause()
        headPoseFaceDetectionProcessor.close()
        headPoseViewModel!!.reset()
    }

    override fun onResume() {
        super.onResume()
        headPoseFaceDetectionProcessor.start()
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