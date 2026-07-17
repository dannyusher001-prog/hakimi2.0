package com.example.analyzer

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.FaceItem
import com.example.data.PhotoCategory
import kotlin.math.abs

object ImageAnalyzer {

    /**
     * Real Average Hash (aHash) algorithm for similarity comparison.
     * 1. Reduce size to 8x8.
     * 2. Reduce color to grayscale.
     * 3. Compute the average grayscale value.
     * 4. Produce a 64-bit hash (each bit represents if pixel is above/below average).
     */
    fun calculateAverageHash(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val pixels = IntArray(64)
        resized.getPixels(pixels, 0, 8, 0, 0, 8, 8)

        val grayValues = IntArray(64)
        var sum = 0
        for (i in 0 until 64) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            // Luma formula for grayscale
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            grayValues[i] = gray
            sum += gray
        }

        val average = sum / 64
        var hash = 0L
        for (i in 0 until 64) {
            if (grayValues[i] >= average) {
                hash = hash or (1L shl i)
            }
        }
        resized.recycle()
        return String.format("%016X", hash)
    }

    /**
     * Compute Hamming distance between two aHash strings.
     * Distance range: 0 (identical) to 64 (totally different).
     * Usually distance <= 10 is considered highly similar.
     */
    fun calculateHammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != 16 || hash2.length != 16) return 64
        try {
            val val1 = hash1.toULong(16)
            val val2 = hash2.toULong(16)
            return (val1 xor val2).countOneBits()
        } catch (e: Exception) {
            return 64
        }
    }

    /**
     * Real Pixel-based Heuristic Image Categorization.
     * Analyzes image pixel color distributions and edge densities to estimate category.
     */
    fun classifyImage(bitmap: Bitmap): PhotoCategory {
        val width = bitmap.width
        val height = bitmap.height
        
        // Sample 15x15 grid of pixels to keep it fast and responsive
        val stepX = (width / 15).coerceAtLeast(1)
        val stepY = (height / 15).coerceAtLeast(1)
        
        var greenCount = 0
        var blueSkyCount = 0
        var skinCount = 0
        var whiteCount = 0
        var warmFoodCount = 0
        var darkCount = 0
        var totalPixels = 0

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                totalPixels++

                // Grayscale check
                val isGrayscale = abs(r - g) < 15 && abs(g - b) < 15 && abs(r - b) < 15
                
                if (r > 230 && g > 230 && b > 230) {
                    whiteCount++
                } else if (r < 40 && g < 40 && b < 40) {
                    darkCount++
                } else if (g > r * 1.15 && g > b * 1.15) {
                    // Predominantly green (nature, plants)
                    greenCount++
                } else if (b > r * 1.15 && b > g * 0.95 && b > 100) {
                    // Predominantly blue (sky, water)
                    blueSkyCount++
                } else if (r > 100 && g > 60 && g < r * 0.85 && b > 40 && b < g * 0.95 && !isGrayscale) {
                    // Skin color tones ranges (roughly)
                    skinCount++
                } else if (r > 150 && g > 100 && g < r && b < 100 && !isGrayscale) {
                    // Warm golden / red (often food, pastries, warm fruit)
                    warmFoodCount++
                }
            }
        }

        val greenRatio = greenCount.toFloat() / totalPixels
        val blueSkyRatio = blueSkyCount.toFloat() / totalPixels
        val skinRatio = skinCount.toFloat() / totalPixels
        val whiteRatio = whiteCount.toFloat() / totalPixels
        val warmRatio = warmFoodCount.toFloat() / totalPixels
        val darkRatio = darkCount.toFloat() / totalPixels

        return when {
            skinRatio > 0.12 -> PhotoCategory.PEOPLE
            whiteRatio > 0.45 || (whiteRatio > 0.25 && darkRatio > 0.15) -> PhotoCategory.DOCUMENT
            greenRatio > 0.15 || blueSkyRatio > 0.20 || (greenRatio + blueSkyRatio) > 0.25 -> PhotoCategory.LANDSCAPE
            warmRatio > 0.15 -> PhotoCategory.FOOD
            darkRatio > 0.40 -> PhotoCategory.ARCHITECTURE
            else -> PhotoCategory.OTHER
        }
    }

    /**
     * Compute cosine similarity or Euclidean distance between FaceNet embeddings.
     * Embedding size: 128-D float vector.
     */
    fun calculateFaceDistance(emb1: FloatArray, emb2: FloatArray): Float {
        if (emb1.size != emb2.size) return 2.0f
        var diffSum = 0.0f
        for (i in emb1.indices) {
            val diff = emb1[i] - emb2[i]
            diffSum += diff * diff
        }
        return kotlin.math.sqrt(diffSum) // Euclidean distance. Lower is more similar. Threshold is typically ~0.8
    }
}
