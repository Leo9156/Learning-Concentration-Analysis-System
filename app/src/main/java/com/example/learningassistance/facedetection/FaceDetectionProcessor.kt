package com.example.learningassistance.facedetection

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.learningassistance.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionProcessor(
    private val context: Context,
    private val fgOverlay: FaceGraphicOverlayView,
    private val tvEulerX: TextView,
    private val tvEulerY: TextView,
    private val tvEulerZ: TextView,
    private val tvRightEyeOpenProb: TextView,
    private val tvLeftEyeOpenProb: TextView,
    private val tvLatencyTime: TextView,
    private val tvDrowsinessMsg: TextView,
    private val tvDrowsinessTimer: TextView,
    ): ImageAnalysis.Analyzer {

    private var totalFrameCount = 0
    private var closeEyesFrameCount = 0
    private var startDrowsinessTimerMs = System.currentTimeMillis()

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Get the image
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val rotation = imageProxy.imageInfo.rotationDegrees
            val detectionStartTimeMs = System.currentTimeMillis()

            Log.v(TAG, "rotation: $rotation")
            // Process the image
            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "Number of face detected: ${faces.size}")

                    // Calculate the processing time
                    val detectionEndTimeMs = System.currentTimeMillis()
                    processTime(detectionStartTimeMs, detectionEndTimeMs)

                    if (faces.size == 0) {
                        setNoFaceMsg()
                    }

                    // Set the information about the face graphic overlay
                    setFaceGraphicOverlay(faces, image, rotation)

                    for (face in faces) {
                        // Get the euler angles of the detected face
                        val rotX = face.headEulerAngleX
                        val rotY = face.headEulerAngleY
                        val rotZ = face.headEulerAngleZ
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                        val leftEyeOpenProb = face.leftEyeOpenProbability

                        // Set the euler angles on the textView
                        setEulerAnglesMsg(rotX, rotY, rotZ)

                        //Set the open probability of each eye on the textView
                        if (rightEyeOpenProb != null) {
                            setRightEyeMsg(rightEyeOpenProb)
                        }
                        if (leftEyeOpenProb != null) {
                            setLeftEyeMsg(leftEyeOpenProb)
                        }

                        // Drwosiness detection
                        totalFrameCount++
                        drowsinessDetection(rightEyeOpenProb!!.toFloat(), leftEyeOpenProb!!.toFloat())
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Face detector failed. $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

    }


    private fun drowsinessDetection(rightEyeOpenProb: Float, leftEyeOpenProb: Float) {
        val endDrowsinessTimerMs = System.currentTimeMillis()
        val duration = endDrowsinessTimerMs - startDrowsinessTimerMs

        if (leftEyeOpenProb < 0.3 && rightEyeOpenProb < 0.3) {
            closeEyesFrameCount++
        }

        // Show the timer
        tvDrowsinessTimer.text = context.getString(R.string.drowsiness_timer, duration / 1000)

        if (duration >= 30000) {
            val PERCLOS = closeEyesFrameCount.toFloat() / totalFrameCount.toFloat()

            if (PERCLOS <= 0.15) {
                tvDrowsinessMsg.text = context.getString(R.string.drowsiness_state_awake, PERCLOS)
                tvDrowsinessMsg.setTextColor(context.getColor(R.color.lime_green))
            } else if (PERCLOS > 0.15 && PERCLOS <= 0.3) {
                tvDrowsinessMsg.text = context.getString(R.string.drowsiness_state_tired, PERCLOS)
                tvDrowsinessMsg.setTextColor(context.getColor(R.color.orange))
            } else {
                tvDrowsinessMsg.text = context.getString(R.string.drowsiness_state_exhausted, PERCLOS)
                tvDrowsinessMsg.setTextColor(context.getColor(R.color.red))
            }

            // Reset the variables
            totalFrameCount = 0
            closeEyesFrameCount = 0
            startDrowsinessTimerMs = System.currentTimeMillis()
        }
    }

    private fun processTime(detectionStartTimeMs: Long, detectionEndTimeMs: Long) {
        val latencyMs = detectionEndTimeMs - detectionStartTimeMs
        val latencyMsg =context.getString(R.string.face_detector_latency) + "$latencyMs" + " ms"
        tvLatencyTime.text = latencyMsg
    }

    private fun setNoFaceMsg() {
        tvEulerX.text = context.getString(R.string.no_face_detected_info)
        tvEulerY.text = context.getString(R.string.no_face_detected_info)
        tvEulerZ.text = context.getString(R.string.no_face_detected_info)
        tvRightEyeOpenProb.text = context.getString(R.string.no_face_detected_info)
        tvLeftEyeOpenProb.text = context.getString(R.string.no_face_detected_info)
    }

    private fun setFaceGraphicOverlay(faces: List<Face>, image: InputImage, rotation: Int) {
        fgOverlay.setFace(faces)
        fgOverlay.setRotation(rotation)
        fgOverlay.setImageSize(image.width.toFloat(), image.height.toFloat())
        fgOverlay.setPreviewSize(fgOverlay.width.toFloat(), fgOverlay.height.toFloat())
    }


    private fun setEulerAnglesMsg(rotX: Float, rotY: Float, rotZ: Float) {
        val eulerXMsg = context.getString(R.string.eulerx) + "$rotX"
        val eulerYMsg = context.getString(R.string.eulery) + "$rotY"
        val eulerZMsg = context.getString(R.string.eulerz) + "$rotZ"
        tvEulerX.text = eulerXMsg
        tvEulerY.text = eulerYMsg
        tvEulerZ.text = eulerZMsg
    }

    private fun setLeftEyeMsg(leftEyeOpenProb: Float) {
        val leftEyeOpenMsg = context.getString(R.string.right_eye_open) + "%.2f".format(leftEyeOpenProb)
        tvLeftEyeOpenProb.text = leftEyeOpenMsg
    }

    private fun setRightEyeMsg(rightEyeOpenProb: Float) {
        val rightEyeOpenMsg = context.getString(R.string.left_eye_open) + "%.2f".format(rightEyeOpenProb)
        tvRightEyeOpenProb.text = rightEyeOpenMsg
    }

    companion object {
        private const val TAG = "FaceDetectionProcessor"
    }

}