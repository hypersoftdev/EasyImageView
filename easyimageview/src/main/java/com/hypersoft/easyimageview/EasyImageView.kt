package com.hypersoft.easyimageview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class EasyImageView @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var cornerRadii = FloatArray(8) { 0f } // Each corner's radius (top-left, top-right, bottom-right, bottom-left)
    private var strokeColor = Color.BLACK
    private var strokeWidth = 0f
    private var strokeGradientStartColor = Color.TRANSPARENT
    private var strokeGradientEndColor = Color.TRANSPARENT
    private var strokeGradientAngle = 0f
    private var selectionIcon: Drawable? = null
    private var selectionIconSize = 0f
    private var selectionIconPosition = Position.TOP_RIGHT
    private var iconPadding = 0f

    private var isRippleEffectEnabled = false

    private var isFlippedHorizontally = false
    private var isFlippedVertically = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rectF = RectF()
    private var gradientShader: Shader? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.EasyImageView, 0, 0).apply {
            try {
                setCornerRadii(
                    getFloat(R.styleable.EasyImageView_cornerRadiusTopLeft, 0f),
                    getFloat(R.styleable.EasyImageView_cornerRadiusTopRight, 0f),
                    getFloat(R.styleable.EasyImageView_cornerRadiusBottomRight, 0f),
                    getFloat(R.styleable.EasyImageView_cornerRadiusBottomLeft, 0f)
                )
                setStrokeColor(getColor(R.styleable.EasyImageView_strokeColor, Color.BLACK))
                setStrokeWidth(getDimension(R.styleable.EasyImageView_strokeWidth, 0f))
                setStrokeGradient(
                    getColor(R.styleable.EasyImageView_strokeGradientStartColor, Color.TRANSPARENT),
                    getColor(R.styleable.EasyImageView_strokeGradientEndColor, Color.TRANSPARENT)
                )
                setStrokeGradientAngle(getFloat(R.styleable.EasyImageView_strokeGradientAngle, 0f))

                setSelectionIcon(
                    getDrawable(R.styleable.EasyImageView_selectionIcon),
                    getDimension(R.styleable.EasyImageView_selectionIconSize, 24f),
                    Position.entries[getInt(R.styleable.EasyImageView_selectionIconPosition, 1)],
                    getDimension(R.styleable.EasyImageView_iconPadding, 8f)
                )

                setFlipHorizontally(getBoolean(R.styleable.EasyImageView_flipHorizontally, false))
                setFlipVertically(getBoolean(R.styleable.EasyImageView_flipVertically, false))

                setRippleEffectEnabled(getBoolean(R.styleable.EasyImageView_enableRippleEffect, false))

            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        updateRectF()

        val matrix = Matrix()
        if (isFlippedHorizontally) {
            matrix.postScale(-1f, 1f, (width / 2).toFloat(), (height / 2).toFloat())
        }
        if (isFlippedVertically) {
            matrix.postScale(1f, -1f, (width / 2).toFloat(), (height / 2).toFloat())
        }

        canvas.concat(matrix)

        canvas.save()
        path.reset()
        path.addRoundRect(rectF, cornerRadii, Path.Direction.CW)
        canvas.clipPath(path)
        super.onDraw(canvas)
        canvas.restore()

        drawSelectionIcon(canvas)
        drawStroke(canvas)
    }

    private fun drawSelectionIcon(canvas: Canvas) {
        selectionIcon?.let { icon ->
            val iconSize = selectionIconSize.toInt()
            val (left, top) = calculateIconPosition(iconSize)

            icon.setBounds(left, top, left + iconSize, top + iconSize)
            icon.draw(canvas)
        }
    }

    private fun calculateIconPosition(iconSize: Int): Pair<Int, Int> {
        val offset = (strokeWidth + iconPadding).toInt()
        return when (selectionIconPosition) {
            Position.TOP_LEFT -> Pair(paddingLeft + offset, paddingTop + offset)
            Position.TOP_RIGHT -> Pair(width - paddingRight - iconSize - offset, paddingTop + offset)
            Position.BOTTOM_LEFT -> Pair(paddingLeft + offset, height - paddingBottom - iconSize - offset)
            Position.BOTTOM_RIGHT -> Pair(width - paddingRight - iconSize - offset, height - paddingBottom - iconSize - offset)
        }
    }

    private fun updateRectF() {
        val halfStrokeWidth = strokeWidth / 2
        rectF.set(
            halfStrokeWidth,
            halfStrokeWidth,
            width.toFloat() - halfStrokeWidth,
            height.toFloat() - halfStrokeWidth
        )
    }

    private fun drawStroke(canvas: Canvas) {
        if (strokeWidth > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.shader = getStrokeShader()
            path.reset()
            path.addRoundRect(rectF, cornerRadii, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }
    }

    private fun getStrokeShader(): Shader? {
        return if (strokeGradientStartColor != Color.TRANSPARENT && strokeGradientEndColor != Color.TRANSPARENT) {
            createGradientShader()
        } else {
            paint.color = if (strokeGradientStartColor != Color.TRANSPARENT) {
                strokeGradientStartColor
            } else if (strokeGradientEndColor != Color.TRANSPARENT) {
                strokeGradientEndColor
            } else {
                strokeColor
            }
            null
        }
    }

    private fun createGradientShader(): Shader? {
        val (x0, y0, x1, y1) = calculateGradientPoints()
        gradientShader = LinearGradient(
            x0, y0, x1, y1,
            strokeGradientStartColor,
            strokeGradientEndColor,
            Shader.TileMode.CLAMP
        )
        return gradientShader
    }

    private fun calculateGradientPoints(): FloatArray {
        val radians = Math.toRadians(strokeGradientAngle.toDouble())
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.hypot(width.toDouble(), height.toDouble()).toFloat() / 2f

        val x0 = (centerX - radius * cos(radians)).toFloat()
        val y0 = (centerY - radius * sin(radians)).toFloat()
        val x1 = (centerX + radius * cos(radians)).toFloat()
        val y1 = (centerY + radius * sin(radians)).toFloat()

        return floatArrayOf(x0, y0, x1, y1)
    }

    fun setCornerRadii(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
        cornerRadii[0] = topLeft
        cornerRadii[1] = topLeft
        cornerRadii[2] = topRight
        cornerRadii[3] = topRight
        cornerRadii[4] = bottomRight
        cornerRadii[5] = bottomRight
        cornerRadii[6] = bottomLeft
        cornerRadii[7] = bottomLeft
        invalidateView()
    }

    fun setStrokeColor(color: Int) {
        strokeColor = color
        invalidateView()
    }

    fun setStrokeWidth(width: Float) {
        strokeWidth = width
        invalidateView()
    }

    fun setStrokeGradient(startColor: Int, endColor: Int) {
        strokeGradientStartColor = startColor
        strokeGradientEndColor = endColor
        gradientShader = null
        invalidateView()
    }

    fun setStrokeGradientAngle(angle: Float) {
        strokeGradientAngle = angle
        gradientShader = null
        invalidateView()
    }

    private fun setSelectionIcon(drawable: Drawable?, size: Float = 24f, position: Position = Position.TOP_RIGHT, padding: Float = 4f) {
        selectionIcon = drawable
        selectionIconSize = size
        selectionIconPosition = position
        iconPadding = padding
        invalidateView()
    }

    fun setSelectionIcon(resId: Int) {
        selectionIcon = ContextCompat.getDrawable(context, resId)
        invalidateView()
    }

    fun setFlipHorizontally(flip: Boolean) {
        isFlippedHorizontally = flip
        invalidateView()
    }

    fun setFlipVertically(flip: Boolean) {
        isFlippedVertically = flip
        invalidateView()
    }

    fun setRippleEffectEnabled(enabled: Boolean) {
        isRippleEffectEnabled = enabled
        if (enabled) {
            addRippleEffect()
        } else {
            foreground = null
            isClickable = false
            isFocusable = false
        }
        invalidateView()
    }


    private fun addRippleEffect() {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val rippleDrawable = ResourcesCompat.getDrawable(resources, typedValue.resourceId, context.theme)
        foreground = rippleDrawable
        isClickable = true
        isFocusable = true
    }

    fun getImageBitmap(): Bitmap? {
        val drawable = drawable ?: return null

        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null) {
                return bitmap
            }
        }

        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight

        if (width <= 0 || height <= 0) {
            return null
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        return bitmap
    }

    fun saveImageToGallery(successMessage:String?,errorMessage:String?,imageName:String?) {
        val bitmap = getImageBitmap()
        saveBitmapToGallery(bitmap,successMessage,errorMessage,imageName)
    }

    private fun saveBitmapToGallery(bitmap: Bitmap?,successMessage:String?,errorMessage:String?,imageName:String?) {
        val filename = imageName?.let {
            "${it}.png"
        }?:run {
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        }
        var fos: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = File(imagesDir, filename)
                fos = FileOutputStream(imageFile)
            }

            fos?.let {
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fos) ?: run {
                    post {
                        Toast.makeText(context, successMessage?:"Image is saved in gallery.", Toast.LENGTH_SHORT).show()
                    }
                }

            } ?: run {
                post {
                    Toast.makeText(context, errorMessage?:"Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            post {
                Toast.makeText(context, errorMessage?:"Failed to save image", Toast.LENGTH_SHORT).show()

            }
        } finally {
            fos?.close()
        }
    }


    private fun invalidateView() {
        invalidate()
        requestLayout()
    }

    enum class Position {
        TOP_RIGHT, TOP_LEFT, BOTTOM_LEFT, BOTTOM_RIGHT
    }


}
