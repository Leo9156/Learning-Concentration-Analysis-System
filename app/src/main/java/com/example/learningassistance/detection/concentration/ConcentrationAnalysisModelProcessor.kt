package com.example.learningassistance.detection.concentration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.example.learningassistance.detection.concentration.yolov5.ImageProcess
import com.example.learningassistance.detection.concentration.yolov5.Results
import com.example.learningassistance.detection.concentration.yolov5.Yolov5TFliteDetector
import com.example.learningassistance.distractionDetectionHelper.DrowsinessDetectionHelper
import com.example.learningassistance.distractionDetectionHelper.HeadPoseAttentionAnalysisHelper
import com.example.learningassistance.distractionDetectionHelper.NoFaceDetectionHelper
import com.example.learningassistance.graphicOverlay.FaceDetectionGraphicOverlay
import com.example.learningassistance.graphicOverlay.FaceMeshGraphicOverlay
import com.example.learningassistance.graphicOverlay.ObjectDetectionGraphicOverlay
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ConcentrationAnalysisModelProcessor(
    context: Context,
    private val yolov5TFliteDetector: Yolov5TFliteDetector,
    var headEulerOffsetX: Float,
    var headEulerOffsetY: Float,
    var closedEyesThreshold: Float,
    private val faceGraphicOverlay: FaceDetectionGraphicOverlay,
    private val faceMeshGraphicOverlay: FaceMeshGraphicOverlay,
    private val objectDetectionGraphicOverlay: ObjectDetectionGraphicOverlay,
    ) : ImageAnalysis.Analyzer {

    // Face detector
    private val faceDetectionOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.8f)
        .build()
    private var faceDetectionDetector: FaceDetector? = null

    // Face mesh detector
    private val faceMeshOptions = FaceMeshDetectorOptions.Builder()
    private var faceMeshDetector: FaceMeshDetector? = null

    /* Object Detection
     * Contains some necessary variables for 3C object detection
     */
    private val imageProcess = ImageProcess()
    private var objectDetectionScope: CoroutineScope? = null
    val isElectronicDevicesDetected = MutableLiveData<String>("")
    private var objectDetectionWindowSize = 10
    private var totalFrame = 0
    private var totalObjectDetectedFrame = 0
    private var objectDetectedFrame = 0
    private var objectNotDetectedFrame = 0
    var isObjectDetectionShouldStart = false

    // Drowsiness detector
    val drowsinessDetector = DrowsinessDetectionHelper(context)

    // No face detector
    val noFaceDetector = NoFaceDetectionHelper(context)

    // head pose analyzer
    val headPoseAttentionAnalyzer = HeadPoseAttentionAnalysisHelper(context)

    // head rotation
    val rotX = MutableLiveData<Float?>(null)
    val rotY = MutableLiveData<Float?>(null)

    // Necessary states
    val isFaceDetected = MutableLiveData<Boolean>(false)
    val isEyesOpen = MutableLiveData<Boolean?>(true)
    var isNoFaceDetectionShouldStart = false
    var isDrowsinessDetectionShouldStart = false
    var isHeadPoseAnalysisShouldStart = false
    var isGraphicShow = false

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Image for ML kit
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // bitmap for yolo
            val inputBitmap = createInputBitmapForObjectDetection(imageProxy)

            // Object Detection
            if (!yolov5TFliteDetector.isDetecting) {
                if (objectDetectionScope != null) {
                    if (objectDetectionScope!!.isActive) {
                        objectDetectionScope!!.launch {
                            val results = yolov5TFliteDetector.detect(inputBitmap)
                            setObjectDetectionGraphicOverlay(results, inputBitmap, imageProxy)
                            if (results.size == 0) {
                                isElectronicDevicesDetected.postValue("")
                            } else {
                                for (result in results) {
                                    isElectronicDevicesDetected.postValue(result.name)
                                }
                            }
                            electDevDetection()
                        }
                    }
                }
            }

            if (faceDetectionDetector != null) {
                faceDetectionDetector!!.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.size == 0) {
                            // Change state
                            isFaceDetected.value = false

                            // rotation degree
                            rotX.value = null
                            rotY.value = null
                            setFaceDetectionGraphicOverlay(null, image)
                        } else {
                            // Change state
                            isFaceDetected.value = true

                            // Get the primary face info
                            var maxFaceIndex = 0
                            for (i in faces.indices) {
                                val maxFaceArea = faces[maxFaceIndex].boundingBox.width() * faces[maxFaceIndex].boundingBox.height()
                                val curFaceArea = faces[i].boundingBox.width() * faces[i].boundingBox.height()
                                if (maxFaceArea < curFaceArea) {
                                    maxFaceIndex = i
                                }
                            }
                            // Calculate the normalized rotation degrees
                            rotX.value = (faces[maxFaceIndex].headEulerAngleX - headEulerOffsetX)
                            rotY.value = (faces[maxFaceIndex].headEulerAngleY - headEulerOffsetY)
                            setFaceDetectionGraphicOverlay(faces[maxFaceIndex], image)
                        }

                        // No face detection
                        noFaceDetection()

                        // Head pose analysis
                        headPoseAttentivenessAnalysis(rotX.value ?: 0f, rotY.value ?: 0f)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Face detector failed. $e")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }

            // Face mesh
            if (faceMeshDetector != null) {
                faceMeshDetector!!.process(image)
                    .addOnSuccessListener { faceMeshes ->
                        if (faceMeshes.size == 0) {
                            // Change state
                            drowsinessDetector.resetEAR()
                            setFaceMeshGraphicOverlay(null, image)
                        } else {
                            var maxFaceIndex = 0
                            for (i in faceMeshes.indices) {
                                val maxFaceArea = faceMeshes[maxFaceIndex].boundingBox.width() * faceMeshes[maxFaceIndex].boundingBox.height()
                                val curFaceArea = faceMeshes[i].boundingBox.width() * faceMeshes[i].boundingBox.height()
                                if (curFaceArea > maxFaceArea) {
                                    maxFaceIndex = i
                                }
                            }
                            drowsinessDetector.calculateEAR(faceMeshes[maxFaceIndex].allPoints)
                            drowsinessDetector.calculateMOR(faceMeshes[maxFaceIndex].allPoints)
                            setFaceMeshGraphicOverlay(faceMeshes[maxFaceIndex], image)
                        }

                        drowsinessDetection()
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Face mesh failed. $e")
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    fun start() {
        if (faceDetectionDetector == null) {
            faceDetectionDetector = FaceDetection.getClient(faceDetectionOptions)
        }
        if (faceMeshDetector == null) {
            faceMeshDetector = FaceMeshDetection.getClient(faceMeshOptions.build())
        }
        if (objectDetectionScope == null) {
            objectDetectionScope = CoroutineScope(Dispatchers.Default)
        }
    }

    fun close() {
        if (faceDetectionDetector != null) {
            faceDetectionDetector!!.close()
            faceDetectionDetector = null

            noFaceDetector.resetDetector()
            noFaceDetector.isNoFaceDetecting = false

            headPoseAttentionAnalyzer.isHeadPoseAnalyzing = false
            headPoseAttentionAnalyzer.resetAnalyzer()
            headPoseAttentionAnalyzer.isDistracted = false
        }
        if (faceMeshDetector != null) {
            faceMeshDetector!!.close()
            faceMeshDetector = null

            drowsinessDetector.resetDetector()
            drowsinessDetector.isDrowsinessAnalyzing = false
        }
        if (objectDetectionScope != null) {
            objectDetectionScope!!.cancel()
            objectDetectionScope = null
        }
        yolov5TFliteDetector.resetState()
    }

    private fun createInputBitmapForObjectDetection(imageProxy: ImageProxy): Bitmap {
        imageProcess.resetYuvBytes()
        imageProcess.fillBytes(imageProxy.planes)
        val rgbBytes = IntArray(imageProxy.width * imageProxy.height)
        imageProcess.yuv420ToARGB8888(
            imageProxy.width,
            imageProxy.height,
            imageProxy.planes[0].rowStride,
            imageProxy.planes[1].rowStride,
            imageProxy.planes[1].pixelStride,
            rgbBytes
        )
        val originalBitmap =
            Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        originalBitmap.setPixels(
            rgbBytes,
            0,
            imageProxy.width,
            0,
            0,
            imageProxy.width,
            imageProxy.height
        )
        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            Yolov5TFliteDetector.INPUT_SIZE.width,
            Yolov5TFliteDetector.INPUT_SIZE.height,
            true
        )
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }

    private fun drowsinessDetection() {
        if (isDrowsinessDetectionShouldStart) {
            drowsinessDetector.increaseTotalFrameNumber()

            if (drowsinessDetector.getEAR() < closedEyesThreshold) {
                drowsinessDetector.increaseClosedEyesFrameNumber()
                isEyesOpen.value = false
            } else {
                if (drowsinessDetector.getEAR() == 2.0f) {
                    isEyesOpen.value = null
                } else {
                    isEyesOpen.value = true
                }
            }

            if (drowsinessDetector.getMOR() > drowsinessDetector.getYawningThreshold()/*set threshold*/) {
                if(!drowsinessDetector.getYawningStatus()) {
                    drowsinessDetector.setYawningStatus(true)
                    drowsinessDetector.startYawningTimer()
                }
                /*
                *       if yawning is false set to true and start timer
                * */
                drowsinessDetector.increaseOpenMouthNumber()
            }
            else {
                if(drowsinessDetector.getYawningStatus())
                {
                    drowsinessDetector.setYawningStatus(false)
                    drowsinessDetector.endYawningTimer()
                    drowsinessDetector.calculateYawningDetectDuration()
                    if(drowsinessDetector.getYawningDetectionDuration()>3000) {
                        drowsinessDetector.addTotalYawningPeriod()
                    }
                }
                /*
                * if yawning is true set to false, if the yawning duration larger than 5 sec add it to total duration
                *
                * */
            }

        }
    }

    private fun noFaceDetection() {
        if (isNoFaceDetectionShouldStart) {
            // Necessary steps
            noFaceDetector.increaseTotalFrameNumber()

            // No face detection
            if (!isFaceDetected.value!!) {
                noFaceDetector.increaseNoFaceFrameNumber()
            }
        }
    }

    private fun headPoseAttentivenessAnalysis(eulerX: Float, eulerY: Float) {
        if (isHeadPoseAnalysisShouldStart) {
            if (!isFaceDetected.value!!) {
                headPoseAttentionAnalyzer.analyzeHeadPose(0f, 0f)
            } else {
                headPoseAttentionAnalyzer.analyzeHeadPose(eulerX, eulerY)
            }
            headPoseAttentionAnalyzer.evaluateAttention()
        }
    }

    private fun electDevDetection() {
        if (isObjectDetectionShouldStart) {
            totalFrame++

            if (isElectronicDevicesDetected.value != "") {
                objectDetectedFrame++
            } else {
                objectNotDetectedFrame++
            }

            if (objectDetectedFrame + objectNotDetectedFrame == objectDetectionWindowSize) {
                if (objectDetectedFrame > objectNotDetectedFrame) {
                    totalObjectDetectedFrame += objectDetectedFrame
                }
                objectNotDetectedFrame = 0
                objectDetectedFrame = 0
            }
        }
    }
    fun calculatePerDetected(): Float {
        return totalObjectDetectedFrame.toFloat() / totalFrame.toFloat()
    }
    fun resetDetector() {
        totalFrame = 0
        totalObjectDetectedFrame = 0
        objectNotDetectedFrame = 0
        objectDetectedFrame = 0
    }

    private fun setFaceDetectionGraphicOverlay(face: Face?, image: InputImage) {
        if (isGraphicShow) {
            faceGraphicOverlay.setFace(face)
            faceGraphicOverlay.setTransformationInfo(image.width, image.height, image.rotationDegrees)
        }
    }

    private fun setFaceMeshGraphicOverlay(faceMesh: FaceMesh?, image: InputImage) {
        if (isGraphicShow) {
            faceMeshGraphicOverlay.setFace(faceMesh)
            faceMeshGraphicOverlay.setTransformationInfo(image.width, image.height, image.rotationDegrees)
        }
    }

    private fun setObjectDetectionGraphicOverlay(objectDetectionResults: ArrayList<Results>, inputBitmap: Bitmap, imageProxy: ImageProxy) {
        if (isGraphicShow) {
            objectDetectionGraphicOverlay.setResult(objectDetectionResults)
            objectDetectionGraphicOverlay.setTransformationInfo(inputBitmap.width, inputBitmap.height, imageProxy)
        }
    }

    companion object {
        private val TAG = "ConcentrationAnalysisFaceProcessor"
    }
}