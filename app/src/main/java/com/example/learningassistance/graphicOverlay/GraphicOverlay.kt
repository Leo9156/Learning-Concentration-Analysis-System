package com.example.learningassistance.graphicOverlay

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.View

open class GraphicOverlay constructor(
    context: Context,
    attrs: AttributeSet
    ): View(context, attrs) {

    // Matrix for transforming from image coordinates to overlay view coordinates.
    private val transformationMatrix = Matrix()

    private var imageWidth = 0
    private var imageHeight = 0
    private var viewWidth = 0
    private var viewHeight = 0

    // The factor of overlay View size to image size. Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of overlay View.
    protected var scaleFactor = 1.0f

    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    protected var postScaleWidthOffset = 0f

    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    protected var postScaleHeightOffset = 0f

    /**
     * Sets the source information of the image being processed by detectors,
     * which informs how to transform image coordinates later.
     *
     * imageWidth is the width of the image sent to ML Kit detectors
     * imageHeight is the height of the image sent to ML Kit detectors
     */
    protected fun setImageInfo(imageWidth: Int, imageHeight: Int) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.viewWidth = width
        this.viewHeight = height
    }

    fun getImageWidth(): Int {
        return imageWidth
    }

    fun getImageHeight(): Int {
        return imageHeight
    }

    /**
     * Adjusts the x coordinate from the image's coordinate system to the view coordinate system.
     */
    protected fun translateX(x: Float): Float {
        return width - (scale(x) - this.postScaleWidthOffset)
        //return scale(x) - postScaleWidthOffset
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    protected fun translateY(y: Float): Float {
        return scale(y) - this.postScaleHeightOffset
    }

    // Adjusts the supplied value from the image scale to the view scale.
    protected fun scale(imagePixel: Float): Float {
        return imagePixel * this.scaleFactor
    }

    protected fun setTransformationElement() {
        val viewAspectRatio = width.toFloat() / height.toFloat()
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = width.toFloat() / imageWidth
            postScaleHeightOffset = (width.toFloat() / imageAspectRatio - height) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = height.toFloat() / imageHeight
            postScaleWidthOffset = (height.toFloat() * imageAspectRatio - width) / 2
        }

        transformationMatrix.reset()
        transformationMatrix.setScale(scaleFactor, scaleFactor)
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)
    }
}