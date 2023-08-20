package com.example.learningassistance.detection.concentration.yolov5

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build.MODEL
import android.util.Log
import android.util.Size
import android.widget.Toast
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.util.Arrays
import java.util.PriorityQueue
import kotlin.time.measureTime

class Yolov5TFliteDetector(private val context: Context) {

    var objectDetectionModel: Interpreter? = null
    private val options = Interpreter.Options()
    private var labels: List<String>? = null
    var isDetecting = false

    fun initModel() {
        // Load the model and the labels
        try {
            val model = FileUtil.loadMappedFile(context, MODEL)
            objectDetectionModel = Interpreter(model, options)
            labels = FileUtil.loadLabels(context, LABEL_FILE)
        } catch (e: IOException) {
            Log.e(TAG, "Load model error: $e")
            Toast.makeText(context, "Load model error: $e", Toast.LENGTH_SHORT).show()
        }
    }

    fun detect(bitmap: Bitmap): ArrayList<Results> {
        isDetecting = true
        val results = ArrayList<Results>()
        var nmsResults: ArrayList<Results> = ArrayList()
        var nmsFilterResults = ArrayList<Results>()

        // Handle input
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE.height, INPUT_SIZE.width, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()
        var input = TensorImage(DataType.FLOAT32)
        input.load(bitmap)
        input = imageProcessor.process(input)

        // Handle output
        val probabilityBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, DataType.FLOAT32)

        // Inference
        if (objectDetectionModel != null) {
            //Log.v(TAG, "Detection started...")
            //val time = measureTime {
                objectDetectionModel!!.run(input.buffer, probabilityBuffer.buffer)
            //}
            //Log.v(TAG, "Detection Finished...")
            //Log.v(TAG, "Infer time: $time")
        }

        // Process inference result
        val resultsArray = probabilityBuffer.floatArray
        for (i in 0 until OUTPUT_SIZE[1]) {
            val gridStride = i * OUTPUT_SIZE[2]
            val x: Float = resultsArray[0 + gridStride] * INPUT_SIZE.width
            val y: Float = resultsArray[1 + gridStride] * INPUT_SIZE.height
            val w: Float = resultsArray[2 + gridStride] * INPUT_SIZE.width
            val h: Float = resultsArray[3 + gridStride] * INPUT_SIZE.height
            val xMin = Math.max(0f, x - w / 2f)
            val yMin = Math.max(0f, y - h / 2f)
            val xMax = Math.min(INPUT_SIZE.width.toFloat(), x + w / 2f)
            val yMax = Math.min(INPUT_SIZE.height.toFloat(), y + h / 2f)
            val confidence: Float = resultsArray[4 + gridStride]
            val classScores: FloatArray =
                Arrays.copyOfRange(resultsArray, 5 + gridStride, OUTPUT_SIZE[2] + gridStride)
            var labelId = 0
            var maxLabelScores = 0f
            for (j in classScores.indices) {
                if (classScores[j] > maxLabelScores) {
                    maxLabelScores = classScores[j]
                    labelId = j
                }
            }

            val r = Results(
                labelId,
                "",
                maxLabelScores,
                confidence,
                RectF(xMin, yMin, xMax, yMax)
            )
            results.add(r)
        }

        // Non maximum suppression
        nms(results, nmsResults)
        nmsFilterResults = nmsAllClass(nmsResults)

        // Update label name
        for (result in nmsFilterResults) {
            val id = result.id
            val labelName = labels!![id]
            result.name = labelName
        }

        isDetecting = false
        return nmsFilterResults
    }

    private fun nms(results: ArrayList<Results>, nmsResults: ArrayList<Results>) {
        for (i in 0 until OUTPUT_SIZE[2] - 5) {
            val pq = PriorityQueue<Results>(
                6300
            ) { l, r ->
                r.confidence.compareTo(l.confidence)
            }


            for (j in results.indices) {
                if (results[j].id == i && results[j].confidence > DETECT_THRESHOLD) {
                    pq.add(results[j])
                }
            }

            while (pq.isNotEmpty()) {
                val detections = pq.toTypedArray()
                val max = detections[0]
                nmsResults.add(max)
                pq.clear()

                for (k in 1 until detections.size) {
                    val detection = detections[k]
                    if (boxIou(max.loc, detection.loc) < IOU_THRESHOLD) {
                        pq.add(detection)
                    }
                }
            }
        }
    }

    private fun nmsAllClass(results: ArrayList<Results>): ArrayList<Results> {
        val nmsResults = ArrayList<Results>()

        val pq = PriorityQueue<Results>(
            100
        ) { l, r ->
            r.confidence.compareTo(l.confidence)
        }

        for (j in 0 until results.size) {
            if (results[j].confidence > DETECT_THRESHOLD) {
                pq.add(results[j])
            }
        }

        while (pq.isNotEmpty()) {
            val detections = pq.toTypedArray()
            val max = detections[0]
            nmsResults.add(max)
            pq.clear()

            for (k in 1 until detections.size) {
                val detection = detections[k]
                if (boxIou(max.loc, detection.loc) < IOU_CLASS_DUPLICATED_THRESHOLD) {
                    pq.add(detection)
                }
            }
        }

        return nmsResults
    }

    private fun boxIou(a: RectF, b: RectF): Float {
        val intersection = boxIntersection(a, b)
        val union = boxUnion(a, b)
        return if (union <= 0) 1f else intersection / union
    }

    private fun boxIntersection(a: RectF, b: RectF): Float {
        val maxLeft = if (a.left > b.left) a.left else b.left
        val maxTop = if (a.top > b.top) a.top else b.top
        val minRight = if (a.right < b.right) a.right else b.right
        val minBottom = if (a.bottom < b.bottom) a.bottom else b.bottom
        val w = minRight - maxLeft
        val h = minBottom - maxTop
        return if (w < 0 || h < 0) 0f else w * h
    }

    private fun boxUnion(a: RectF, b: RectF): Float {
        val i = boxIntersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    fun addGPUDelegate() {
        val compatibilityList = CompatibilityList()
        if (compatibilityList.isDelegateSupportedOnThisDevice) {
            val delegateOptions = compatibilityList.bestOptionsForThisDevice
            val gpuDelegate = GpuDelegate(delegateOptions)
            options.addDelegate(gpuDelegate)
        } else {
            addThread(4)
        }
    }

    private fun addThread(num: Int) {
        options.numThreads = num
    }

    fun resetState() {
        this.isDetecting = false
    }

    companion object {
        private const val TAG = "Yolov5Detector"
        val INPUT_SIZE = Size(640, 640)
        val OUTPUT_SIZE = intArrayOf(1, 25200, 7)
        const val DETECT_THRESHOLD = 0.7f
        const val IOU_THRESHOLD = 0.3f
        const val IOU_CLASS_DUPLICATED_THRESHOLD = 0.7f
        const val MODEL = "3C_yolov5n-dynamic-640.tflite"
        const val LABEL_FILE = "3C.txt"
    }
}