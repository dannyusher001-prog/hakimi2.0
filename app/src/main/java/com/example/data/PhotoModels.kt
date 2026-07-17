package com.example.data

import android.net.Uri

enum class PhotoCategory(val key: String) {
    PEOPLE("people"),
    LANDSCAPE("landscape"),
    ANIMAL("animal"),
    FOOD("food"),
    VEHICLE("vehicle"),
    ARCHITECTURE("architecture"),
    DOCUMENT("document"),
    SPORTS("sports"),
    OTHER("other");

    fun getDisplayName(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                PEOPLE -> "People"
                LANDSCAPE -> "Nature & Landscape"
                ANIMAL -> "Animals"
                FOOD -> "Food"
                VEHICLE -> "Vehicles & Transport"
                ARCHITECTURE -> "Architecture"
                DOCUMENT -> "Docs & Screenshots"
                SPORTS -> "Sports"
                OTHER -> "Others"
            }
        } else {
            when (this) {
                PEOPLE -> "人物"
                LANDSCAPE -> "风景/自然"
                ANIMAL -> "动物"
                FOOD -> "食物"
                VEHICLE -> "车辆/交通"
                ARCHITECTURE -> "建筑/城市"
                DOCUMENT -> "文档/截图"
                SPORTS -> "运动"
                OTHER -> "其他"
            }
        }
    }
}

data class FaceItem(
    val id: Int,
    val personName: String,
    val embedding: FloatArray, // Simulated FaceNet 128-D vector
    val boundsLeft: Float = 0.3f,
    val boundsTop: Float = 0.3f,
    val boundsRight: Float = 0.7f,
    val boundsBottom: Float = 0.7f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceItem) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}

data class PhotoItem(
    val id: Long,               // Positive for real photos, negative for demo photos
    val displayName: String,
    val uri: Uri?,              // Null for demo photos
    val size: Long = 1024 * 100,
    val dateAdded: Long = System.currentTimeMillis() / 1000,
    var category: PhotoCategory = PhotoCategory.OTHER,
    val faces: List<FaceItem> = emptyList(),
    val averageHash: String = "0000000000000000" // 64-bit hex representation
)

data class PeopleGroup(
    val id: Int,
    val name: String,
    val snapshotFace: FaceItem,
    val photos: List<PhotoItem>
)

data class SimilarGroup(
    val id: Int,
    val photos: List<PhotoItem>
)

enum class AppLanguage {
    SIMPLIFIED_CHINESE,
    ENGLISH
}
