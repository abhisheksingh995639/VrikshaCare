package com.example.rosehealth

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Classifier(context: Context, modelPath: String, labelsPath: String) {
    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputImageSize = 224

    init {
        Log.d("Classifier", "Loading model: $modelPath")
        val modelBuffer = loadModelFile(context, modelPath)
        Log.d("Classifier", "Model buffer capacity: ${modelBuffer.capacity()} bytes")
        interpreter = Interpreter(modelBuffer)
        Log.d("Classifier", "Interpreter created OK")
        labels = FileUtil.loadLabels(context, labelsPath)
        Log.d("Classifier", "Labels loaded: $labels")
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val inputStream = context.assets.open(modelPath)
        val bytes = inputStream.readBytes()
        inputStream.close()
        // TFLite models use little-endian FlatBuffers
        val buf = ByteBuffer.allocateDirect(bytes.size)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(bytes)
        buf.rewind()
        return buf
    }

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputImageSize, inputImageSize, true)

        // Check what input type the model expects
        val inputType = interpreter.getInputTensor(0).dataType()
        Log.d("Classifier", "Model input type: $inputType")

        val inputBuffer = ByteBuffer.allocateDirect(1 * inputImageSize * inputImageSize * 3 * 4)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val pixels = IntArray(inputImageSize * inputImageSize)
        scaledBitmap.getPixels(pixels, 0, inputImageSize, 0, 0, inputImageSize, inputImageSize)
        
        var sumR = 0f
        var maxR = 0f
        
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF).toFloat()
            val g = ((pixel shr 8) and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()
            
            sumR += r
            if (r > maxR) maxR = r
            
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        
        Log.d("Classifier", "Input statistics - Mean: ${sumR/pixels.size}, Max: $maxR")
        inputBuffer.rewind()

        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.FLOAT32)
        interpreter.run(inputBuffer, outputBuffer.buffer.rewind())

        val probabilities = outputBuffer.floatArray
        Log.d("Classifier", "Raw probabilities: ${probabilities.contentToString()}")
        return labels.mapIndexed { index, label ->
            label to (if (index < probabilities.size) probabilities[index] else 0f)
        }.sortedByDescending { it.second }
    }

    fun close() {
        interpreter.close()
    }
}
