package com.hypersoft.easyimageview


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
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
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EasyImageView @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var cornerRadius = 0f
    private var strokeColor = Color.BLACK
    private var strokeWidth = 0f
    private var strokeGradientStartColor = Color.TRANSPARENT
    private var strokeGradientEndColor = Color.TRANSPARENT
    private var selectionIcon: Drawable? = null
    private var selectionIconSize = 0f
    private var selectionIconPosition = Position.TOP_RIGHT
    private var iconPadding = 0f


    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rectF = RectF()
    private var gradientShader: Shader? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.RoundedImageView, 0, 0).apply {
            try {
                setCornerRadius(getDimension(R.styleable.RoundedImageView_cornerRadius, 0f))
                setStrokeColor(getColor(R.styleable.RoundedImageView_strokeColor, Color.BLACK))
                setStrokeWidth(getDimension(R.styleable.RoundedImageView_strokeWidth, 0f))
                setStrokeGradient(
                    getColor(R.styleable.RoundedImageView_strokeGradientStartColor, Color.TRANSPARENT),
                    getColor(R.styleable.RoundedImageView_strokeGradientEndColor, Color.TRANSPARENT)
                )

                setSelectionIcon(
                    getDrawable(R.styleable.RoundedImageView_selectionIcon),
                    getDimension(R.styleable.RoundedImageView_selectionIconSize, 24f),
                    Position.values()[getInt(R.styleable.RoundedImageView_selectionIconPosition, 1)],
                    getDimension(R.styleable.RoundedImageView_iconPadding, 8f)
                )

            } finally {
                recycle()
            }
        }

    }

    override fun onDraw(canvas: Canvas) {
        // Define bounds for the rounded rectangle (avoid allocation in onDraw)
        updateRectF()

        // Draw the rounded image content within a clipped path
        canvas.save()
        path.reset()
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
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
        val offset = (strokeWidth + iconPadding).toInt() // Offset to avoid overlap with stroke
        return when (selectionIconPosition) {
            Position.TOP_LEFT -> Pair(paddingLeft + offset, paddingTop + offset)
            Position.TOP_RIGHT -> Pair(width - paddingRight - iconSize - offset, paddingTop + offset)
            Position.BOTTOM_LEFT -> Pair(paddingLeft + offset, height - paddingBottom - iconSize - offset)
            Position.BOTTOM_RIGHT -> Pair(width - paddingRight - iconSize - offset, height - paddingBottom - iconSize - offset)
        }
    }

    // Updates the rectF with the current dimensions and stroke width
    private fun updateRectF() {
        val halfStrokeWidth = strokeWidth / 2
        rectF.set(
            halfStrokeWidth,
            halfStrokeWidth,
            width.toFloat() - halfStrokeWidth,
            height.toFloat() - halfStrokeWidth
        )
    }

    // Draws the stroke with the gradient or solid color based on the properties set
    private fun drawStroke(canvas: Canvas) {
        if (strokeWidth > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.shader = getStrokeShader() // Apply the gradient or solid color shader
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        }
    }

    // Returns a shader for the stroke, setting up a gradient or solid color based on the colors provided
    private fun getStrokeShader(): Shader? {
        return if (strokeGradientStartColor != Color.TRANSPARENT && strokeGradientEndColor != Color.TRANSPARENT) {
            if (gradientShader == null) {
                gradientShader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    strokeGradientStartColor,
                    strokeGradientEndColor,
                    Shader.TileMode.CLAMP
                )
            }
            gradientShader
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

    // Programmatically update properties
    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
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
        gradientShader = null // Reset shader to force recalculation
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


    // Method to get a bitmap of the original image content without stroke, radius, or icon
    fun getImageBitmap(): Bitmap? {
        val drawable = drawable ?: return null

        // Check if the drawable is a BitmapDrawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            // Return the original bitmap if available
            if (bitmap != null) {
                return bitmap
            }
        }

        // For other drawable types, use intrinsic dimensions as fallback
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight

        if (width <= 0 || height <= 0) {
            // Invalid dimensions, return null or create a default-sized bitmap
            return null
        }

        // Create a bitmap with the drawable's dimensions
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw the drawable onto the bitmap
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        return bitmap
    }


    fun saveImageToGallery() {
        val bitmap = getImageBitmap()
        saveBitmapToGallery(bitmap)
    }

    private fun saveBitmapToGallery(bitmap: Bitmap?) {
        val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
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
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fos)?:run {
                    post{
                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
                post {
                    Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                }
            }?:run{
                post {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            post {
                Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        } finally {
            fos?.close()
        }
    }

    // Helper method to refresh the view
    private fun invalidateView() {
        invalidate()  // Redraw the view
        requestLayout() // Recalculate layout if necessary
    }

    enum class Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

}