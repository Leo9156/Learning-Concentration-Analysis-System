package com.example.learningassistance.detection.concentration

import android.content.Context
import android.content.Intent
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
import com.example.learningassistance.MainActivity
import com.example.learningassistance.R
import com.example.learningassistance.database.TaskDatabase
import com.example.learningassistance.databinding.FragmentConcentrationAnalysisBinding
import com.example.learningassistance.detection.concentration.yolov5.Yolov5TFliteDetector
import java.util.concurrent.Executors

class ConcentrationAnalysisFragment : Fragment() {
    private var _binding: FragmentConcentrationAnalysisBinding? = null
    private val binding get() = _binding!!

    // Context
    private lateinit var safeContext: Context

    // Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraExecutor = Executors.newSingleThreadExecutor()

    // View model
    private var concentrationAnalysisViewModel: ConcentrationAnalysisViewModel? = null

    // Concentration analysis processor
    private var concentrationAnalysisFaceProcessor: ConcentrationAnalysisModelProcessor? = null
    // private var concentrationAnalysisObjectProcessor: ConcentrationAnalysisObjectProcessor? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.safeContext = context
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = com.example.learningassistance.databinding.FragmentConcentrationAnalysisBinding.inflate(inflater, container, false)
        val view = binding.root

        // Get the basic head rotation
        val headEulerOffsetX = ConcentrationAnalysisFragmentArgs.fromBundle(requireArguments()).basicHeadOffsetX
        val headEulerOffsetY = ConcentrationAnalysisFragmentArgs.fromBundle(requireArguments()).basicHeadOffsetY
        val avgEAR = ConcentrationAnalysisFragmentArgs.fromBundle(requireArguments()).avgEAR
        // Initialize the dao interface of the task database
        val application = requireActivity().application
        val dao = TaskDatabase.getInstance(application).taskDao

        // Yolov5 object detector
        val yolov5TFliteDetector = Yolov5TFliteDetector(safeContext)
        yolov5TFliteDetector.initModel()
        yolov5TFliteDetector.addGPUDelegate()
        Log.v(TAG, "yolov5 model: ${yolov5TFliteDetector.objectDetectionModel}")

        // Initialize face processors
        if (concentrationAnalysisFaceProcessor == null) {
            concentrationAnalysisFaceProcessor =
                ConcentrationAnalysisModelProcessor(
                    safeContext,
                    yolov5TFliteDetector,
                    headEulerOffsetX,
                    headEulerOffsetY,
                    avgEAR * 0.7f,
                    binding.faceDetectionGraphicOverlay,
                    binding.faceMeshGraphicOverlay,
                    binding.objectDetectionGraphicOverlay
                )
        }
        concentrationAnalysisFaceProcessor!!.start()

        Log.v(TAG, "threshold: ${avgEAR * 0.7f}")

        // Initialize object processor
        /*if (concentrationAnalysisObjectProcessor == null) {
            concentrationAnalysisObjectProcessor =
                ConcentrationAnalysisObjectProcessor(
                    safeContext,
                    binding.objectDetectionGraphicOverlay)
        }
        concentrationAnalysisObjectProcessor!!.start()*/

        // Create the view model
        if (concentrationAnalysisViewModel == null) {
            val taskId = requireActivity().intent.extras!!.getLong("taskId")
            val concentrationAnalysisViewModelFactory = ConcentrationAnalysisViewModelFactory(
                safeContext,
                dao,
                taskId,
                concentrationAnalysisFaceProcessor!!,
                //concentrationAnalysisObjectProcessor!!
            )
            concentrationAnalysisViewModel = ViewModelProvider(
                requireActivity(),
                concentrationAnalysisViewModelFactory
            ).get(ConcentrationAnalysisViewModel::class.java)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Update processors of the view model
        concentrationAnalysisViewModel!!.updateProcessor(
            concentrationAnalysisFaceProcessor!!,
            //concentrationAnalysisObjectProcessor!!
        )

        // Detection card -> face
        concentrationAnalysisFaceProcessor!!.isFaceDetected.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.detectionCardFaceText.text = getString(R.string.head_pose_analysis_title_detected)
                binding.detectionCardFaceText.setTextColor(ContextCompat.getColor(safeContext, R.color.green))
                binding.detectionCardFaceIcon.setImageResource(R.drawable.ic_check)
            } else {
                binding.detectionCardFaceText.text = getString(R.string.head_pose_analysis_title_not_detected)
                binding.detectionCardFaceText.setTextColor(ContextCompat.getColor(safeContext, R.color.red))
                binding.detectionCardFaceIcon.setImageResource(R.drawable.ic_error)
            }
        })

        // Detection card -> eyes
        concentrationAnalysisFaceProcessor!!.isEyesOpen.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                binding.detectionCardEyesIcon.visibility = View.INVISIBLE
                binding.detectionCardEyesText.visibility = View.INVISIBLE
            } else {
                binding.detectionCardEyesIcon.visibility = View.VISIBLE
                binding.detectionCardEyesText.visibility = View.VISIBLE
                if (it) {
                    binding.detectionCardEyesIcon.setImageResource(R.drawable.open_eye)
                    binding.detectionCardEyesText.text = getString(R.string.eyes_open)
                    binding.detectionCardEyesText.setTextColor(ContextCompat.getColor(safeContext, R.color.green))
                } else {
                    binding.detectionCardEyesIcon.setImageResource(R.drawable.ic_close_eye)
                    binding.detectionCardEyesText.text = getString(R.string.eyes_close)
                    binding.detectionCardEyesText.setTextColor(ContextCompat.getColor(safeContext, R.color.red))
                }
            }
        })

        // Detection card -> head rotation
        concentrationAnalysisFaceProcessor!!.rotX.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding.detectionCardHeadXText.visibility = View.VISIBLE
                binding.detectionCardHeadXIcon.visibility = View.VISIBLE
                if (it > 20.0f || it < -20.0f) {
                    binding.detectionCardHeadXText.text = String.format(getString(R.string.head_pose_card_eulerx), it)
                    binding.detectionCardHeadXText.setTextColor(ContextCompat.getColor(safeContext, R.color.red))
                    binding.detectionCardHeadXIcon.setImageResource(R.drawable.ic_face_red)
                } else {
                    binding.detectionCardHeadXText.text = String.format(getString(R.string.head_pose_card_eulerx), it)
                    binding.detectionCardHeadXText.setTextColor(ContextCompat.getColor(safeContext, R.color.green))
                    binding.detectionCardHeadXIcon.setImageResource(R.drawable.ic_face_green)
                }
            } else {
                binding.detectionCardHeadXText.visibility = View.INVISIBLE
                binding.detectionCardHeadXIcon.visibility = View.INVISIBLE
            }
        })
        concentrationAnalysisFaceProcessor!!.rotY.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding.detectionCardHeadYText.visibility = View.VISIBLE
                binding.detectionCardHeadYIcon.visibility = View.VISIBLE
                if (it > 20.0f || it < -20.0f) {
                    binding.detectionCardHeadYText.text = String.format(getString(R.string.head_pose_card_eulery), it)
                    binding.detectionCardHeadYText.setTextColor(ContextCompat.getColor(safeContext, R.color.red))
                    binding.detectionCardHeadYIcon.setImageResource(R.drawable.ic_face_red)
                } else {
                    binding.detectionCardHeadYText.text = String.format(getString(R.string.head_pose_card_eulery), it)
                    binding.detectionCardHeadYText.setTextColor(ContextCompat.getColor(safeContext, R.color.green))
                    binding.detectionCardHeadYIcon.setImageResource(R.drawable.ic_face_green)
                }
            } else {
                binding.detectionCardHeadYText.visibility = View.INVISIBLE
                binding.detectionCardHeadYIcon.visibility = View.INVISIBLE
            }
        })

        // Detection card -> electronic devices
        concentrationAnalysisFaceProcessor!!.isElectronicDevicesDetected.observe(viewLifecycleOwner, Observer {
            if (it == "") {
                binding.detectionCardElectronicDevicesText.text = getString(R.string.electronic_devices_not_detected)
                binding.detectionCardElectronicDevicesText.setTextColor(ContextCompat.getColor(safeContext, R.color.green))
                binding.detectionCardElectronicDevicesIcon.setImageResource(R.drawable.ic_cellphone_green)
            } else {
                binding.detectionCardElectronicDevicesText.text = String.format(getString(R.string.electronic_devices_detected), it)
                binding.detectionCardElectronicDevicesText.setTextColor(ContextCompat.getColor(safeContext, R.color.red))
                binding.detectionCardElectronicDevicesIcon.setImageResource(R.drawable.ic_cellphone_red)
            }
        })

        // Setting switch
        binding.detectionGraphicSettingSwitch.setOnCheckedChangeListener { _, isChecked ->
            concentrationAnalysisFaceProcessor!!.isGraphicShow = isChecked
            //concentrationAnalysisObjectProcessor!!.isGraphicShow = isChecked

            if (isChecked) {
                binding.faceDetectionGraphicOverlay.visibility = View.VISIBLE
                binding.faceMeshGraphicOverlay.visibility = View.VISIBLE
                binding.objectDetectionGraphicOverlay.visibility = View.VISIBLE

            } else {
                binding.faceDetectionGraphicOverlay.visibility = View.GONE
                binding.faceMeshGraphicOverlay.visibility = View.GONE
                binding.objectDetectionGraphicOverlay.visibility = View.GONE
            }
        }
        binding.detectionResultSettingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.detectionInfoCard.visibility = View.GONE
            } else {
                binding.detectionInfoCard.visibility = View.VISIBLE
            }
        }

        // Timer
        concentrationAnalysisViewModel!!.task.observe(viewLifecycleOwner, Observer {
            concentrationAnalysisViewModel!!.isTimerShouldStart.value = it != null
        })
        concentrationAnalysisViewModel!!.isTimerShouldStart.observe(viewLifecycleOwner, Observer {
            if (it) {
                concentrationAnalysisViewModel!!.setTimerTime()
                concentrationAnalysisViewModel!!.startTimer()
                concentrationAnalysisViewModel!!.setDistractionValue()
            } else {
                concentrationAnalysisViewModel!!.stopTimer()
            }
        })
        concentrationAnalysisViewModel!!.timeLeftMs.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                val hr = (it / (1000 * 3600)).toInt()
                val min = ((it / (1000 * 60)) % 60).toInt()
                val sec = ((it / 1000) % 60).toInt()
                binding.detectionTimer.text = String.format(getString(R.string.detection_timer), hr, min, sec)
            }
        })

        // Restart head pose button
        binding.restartHeadPoseBtn.setOnClickListener {
            concentrationAnalysisViewModel!!.isPaused.value = null
            this.findNavController().navigate(R.id.action_concentrationAnalysisFragment_to_headPoseMeasureFragment)
        }

        // Three control button
        binding.detectionPause.setOnClickListener {
            concentrationAnalysisViewModel!!.isPaused.value = true
            binding.detectionPlay.isClickable = true
            binding.detectionPause.isClickable = false
        }
        binding.detectionPlay.setOnClickListener {
            concentrationAnalysisViewModel!!.isPaused.value = false
            binding.detectionPlay.isClickable = false
            binding.detectionPause.isClickable = true
        }
        binding.detectionStop.setOnClickListener {
            concentrationAnalysisViewModel!!.isTimerShouldStart.value = false
            val intent = Intent(safeContext, MainActivity::class.java)
            startActivity(intent)
        }
        concentrationAnalysisViewModel!!.isPaused.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                if (it) {
                    // Close the models
                    concentrationAnalysisFaceProcessor!!.close()
                    /*cameraExecutor.execute {
                        concentrationAnalysisObjectProcessor!!.close()
                    }*/
                    // pause the camera
                    cameraProvider.unbindAll()
                    // Stop the timer
                    concentrationAnalysisViewModel!!.isTimerShouldStart.value = false
                } else {
                    // Start the models
                    concentrationAnalysisFaceProcessor!!.start()
                    //concentrationAnalysisObjectProcessor!!.start()
                    // continue the camera
                    startCamera()
                    // Start the timer
                    if (concentrationAnalysisViewModel!!.task.value != null) {
                        concentrationAnalysisViewModel!!.isTimerShouldStart.value = true
                    }
                    // Reset the state
                    concentrationAnalysisViewModel!!.isPaused.value = null
                }
            }
        })

        // Observe whether the task has finished
        concentrationAnalysisViewModel!!.isFinished.observe(viewLifecycleOwner, Observer {
            if (it) {
                // Play sound
                val mediaPlayer = MediaPlayer.create(safeContext, R.raw.basic_head_pose_complete)
                mediaPlayer.setOnCompletionListener {
                    it.release()
                }
                mediaPlayer.start()

                concentrationAnalysisFaceProcessor!!.close()
                /*cameraExecutor.execute {
                    concentrationAnalysisObjectProcessor!!.close()
                }*/
                concentrationAnalysisViewModel!!.isFinished.value = false
                this.findNavController().navigate(R.id.action_concentrationAnalysisFragment_to_analysisResultsFragment)
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
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            }
            else {
                Toast.makeText(safeContext, "Permissions not granted by the user", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalysisYUV = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            /*val imageAnalysisRGB = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()*/

            imageAnalysisYUV.setAnalyzer(
                cameraExecutor,
                concentrationAnalysisFaceProcessor!!
            )
            /*imageAnalysisRGB.setAnalyzer(
                cameraExecutor,
                concentrationAnalysisObjectProcessor!!
            )*/

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysisYUV,
                    //imageAnalysisRGB,
                    preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(safeContext))
    }

    override fun onPause() {
        super.onPause()
        concentrationAnalysisFaceProcessor!!.close()
        /*cameraExecutor.execute {
            concentrationAnalysisObjectProcessor!!.close()
        }*/
        concentrationAnalysisViewModel!!.isTimerShouldStart.value = false
    }

    override fun onResume() {
        super.onResume()
        if (concentrationAnalysisViewModel!!.isPaused.value == null) {
            concentrationAnalysisFaceProcessor!!.start()
            //concentrationAnalysisObjectProcessor!!.start()
            if (concentrationAnalysisViewModel!!.task.value != null) {
                concentrationAnalysisViewModel!!.isTimerShouldStart.value = true
            }
        }
        concentrationAnalysisFaceProcessor!!.headEulerOffsetX = ConcentrationAnalysisFragmentArgs.fromBundle(requireArguments()).basicHeadOffsetX
        concentrationAnalysisFaceProcessor!!.headEulerOffsetY = ConcentrationAnalysisFragmentArgs.fromBundle(requireArguments()).basicHeadOffsetY
        concentrationAnalysisFaceProcessor!!.closedEyesThreshold = (ConcentrationAnalysisFragmentArgs.fromBundle(requireArguments()).avgEAR) * 0.7f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "CameraPreviewFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}