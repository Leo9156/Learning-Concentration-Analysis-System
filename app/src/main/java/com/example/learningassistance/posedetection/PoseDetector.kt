package com.example.learningassistance.posedetection

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseDetector {

    private var options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)

    private val detector: PoseDetector? = null

    //TODO: Create a setter for options

    fun detectInImage(image: InputImage) {

    }
}