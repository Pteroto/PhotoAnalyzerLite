package com.example.android.tflitecamerademo

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NewImageClassifier(private val assetManager: AssetManager) {

    private var interpreter: FirebaseModelInterpreter?
    private var inputOutputOptions: FirebaseModelInputOutputOptions
    private var imageConverter: ImageConverter


    init {
        val localSource = FirebaseLocalModel.Builder("model") // Assign a name to this model
                .setAssetFilePath("model.tflite")
                .build()
        FirebaseModelManager.getInstance().registerLocalModel(localSource)

        val options = FirebaseModelOptions.Builder()
                .setLocalModelName("model")
                .build()
        interpreter = FirebaseModelInterpreter.getInstance(options)

        imageConverter = ImageConverter()

        inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
                .setInputFormat(0, FirebaseModelDataType.BYTE, intArrayOf(1, 224, 224, 3))
                .setOutputFormat(0, FirebaseModelDataType.BYTE, intArrayOf(1, 2))
                .build()
    }


    fun classifyImage(bitmap: Bitmap): List<String> {
        var result = listOf<String>()

        runBlocking {
            result = suspendClassifyImage(bitmap)
        }

        return result
    }


    private suspend fun suspendClassifyImage(bitmap: Bitmap): List<String> = suspendCoroutine { continuation ->
        val resultList = mutableListOf<String>()

        val byteBuffer = imageConverter.convertBitmapToByteBuffer(bitmap)

//        val batchNum = 0
//        val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
//        for (x in 0..223) {
//            for (y in 0..223) {
//                val pixel = bitmap.getPixel(x, y)
//                // Normalize channel values to [-1.0, 1.0]. This requirement varies by
//                // model. For example, some models might require values to be normalized
//                // to the range [0.0, 1.0] instead.
//                input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 255.0f
//                input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 255.0f
//                input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 255.0f
//            }
//        }

        val inputs = FirebaseModelInputs.Builder()
                .add(byteBuffer) // add() as many input arrays as your model requires
                .build()

        interpreter?.run(inputs, inputOutputOptions)?.addOnSuccessListener {
            val output = it.getOutput<Array<ByteArray>>(0)
            val probabilities = output[0]

            val reader = BufferedReader(
                    InputStreamReader(assetManager.open("labels.txt")))
//            for (i in probabilities.indices) {
//                val label = reader.readLine()
//                Log.i("MLKit", String.format("%s: %1.4b", label, probabilities[i]))
//            }

            val labelList = ArrayList<String>()

            labelList.add(reader.readLine())
            labelList.add(reader.readLine())

            val labelProbArray = Array(1) {
                probabilities
            }

            for (i in labelList.indices) {
                sortedLabels.add(
                        AbstractMap.SimpleEntry(labelList[i], (labelProbArray[0][i].toInt() and 0xff) / 255.0f))
                if (sortedLabels.size > 2) {
                    sortedLabels.poll()
                }
            }
            val size = sortedLabels.size

            for (i in 0 until size) {
                val label = sortedLabels.poll()

                resultList.add(label.key + ":" + java.lang.Float.toString(label.value))
            }

            continuation.resume(resultList)
        }
                ?.addOnFailureListener {
                    continuation.resume(resultList)
                    Log.e("MLKit", it.message ?: "")
                }
    }

    private val sortedLabels = PriorityQueue<AbstractMap.SimpleEntry<String, Float>>(
            2,
            Comparator<AbstractMap.SimpleEntry<String, Float>> { o1, o2 -> o1.value.compareTo(o2.value) })
}