package com.example.learningassistance.detection

import androidx.lifecycle.ViewModel

class HeadPoseMeasureViewModel : ViewModel() {
    // Average head rotation degree
    var headEulerXOffset = 0f
    var headEulerYOffset = 0f

    // The state of analysis
    var isAnalysisStarting = false  // Indicate whether the analysis has started
    var hasToRestart = false  // Indicate whether the analysis needed to be restarted
}