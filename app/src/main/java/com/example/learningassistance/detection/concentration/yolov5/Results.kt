package com.example.learningassistance.detection.concentration.yolov5

import android.graphics.RectF


class Results(
    val id: Int,
    var name: String,
    val score: Float,
    val confidence: Float,
    val loc: RectF,
)