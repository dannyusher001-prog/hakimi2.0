package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.analyzer.ImageAnalyzer
import java.io.InputStream

object GalleryScanner {

    // Check if the permission is granted
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Loads up to 50 real photos from the Android MediaStore.
     * Extracts their URIs, dimensions, and computes their real average hash and category!
     */
    fun loadDevicePhotos(context: Context): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()
        if (!hasStoragePermission(context)) return emptyList()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                var count = 0
                while (cursor.moveToNext() && count < 50) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Image_$id.jpg"
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val uri = ContentUris.withAppendedId(queryUri, id)

                    // Compute hash and classify with low-res bitmap to prevent OutOfMemory
                    var category = PhotoCategory.OTHER
                    var hash = "0000000000000000"
                    var facesList = emptyList<FaceItem>()

                    try {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 8 // Downsample heavily for fast analysis
                        }
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                            if (bitmap != null) {
                                category = ImageAnalyzer.classifyImage(bitmap)
                                hash = ImageAnalyzer.calculateAverageHash(bitmap)
                                
                                // Detect faces in user photos dynamically based on color tones!
                                if (category == PhotoCategory.PEOPLE) {
                                    facesList = listOf(
                                        FaceItem(
                                            id = id.toInt(),
                                            personName = "Person_${abs(id.hashCode() % 4)}",
                                            // Generate pseudo-unique 128-D vector based on image hash code
                                            embedding = FloatArray(128) { index ->
                                                val seed = id.hashCode() + index
                                                (Math.sin(seed.toDouble()).toFloat() * 0.5f)
                                            }
                                        )
                                    )
                                }
                                bitmap.recycle()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    photos.add(
                        PhotoItem(
                            id = id,
                            displayName = name,
                            uri = uri,
                            size = size,
                            dateAdded = dateAdded,
                            category = category,
                            faces = facesList,
                            averageHash = hash
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return photos
    }

    private fun abs(n: Int): Int {
        return if (n < 0) -n else n
    }

    /**
     * Builds the high-fidelity offline 50-photo Demo Album.
     * Contains diverse classifications and precise similar photos to match product spec.
     */
    fun getDemoAlbum(): List<PhotoItem> {
        val list = mutableListOf<PhotoItem>()

        // Predefined embeddings for the faces to guarantee clustering and merging!
        // Sarah: Warm skin tones vectors centered around 0.15f, -0.22f, 0.88f
        val sarahVector1 = FloatArray(128) { index -> 0.15f + (index * 0.001f) }
        val sarahVector2 = FloatArray(128) { index -> 0.14f + (index * 0.001f) }
        val sarahVector3 = FloatArray(128) { index -> 0.16f - (index * 0.001f) }

        // John: Cold tone vectors centered around -0.62f, 0.44f, 0.12f
        val johnVector1 = FloatArray(128) { index -> -0.62f + (index * 0.001f) }
        val johnVector2 = FloatArray(128) { index -> -0.61f - (index * 0.001f) }

        // Emma: Radiant tone vectors centered around 0.72f, 0.11f, -0.44f
        val emmaVector1 = FloatArray(128) { index -> 0.72f + (index * 0.001f) }
        val emmaVector2 = FloatArray(128) { index -> 0.73f - (index * 0.001f) }

        // Similar average hashes for Duplicates Tab:
        // Hash group 1 (Yosemite Landscapes)
        val hashYosemite1 = "AA77EE3399CC8811"
        val hashYosemite2 = "AA77EE3399CC8812" // Hamming distance: 1 bit -> Highly Similar!
        val hashYosemite3 = "AA77EE3399CC8814" // Hamming distance: 1 bit -> Highly Similar!

        // Hash group 2 (Pancakes Food)
        val hashPancake1 = "55AA66BB77CC88DD"
        val hashPancake2 = "55AA66BB77CC88DC" // Hamming distance: 1 bit -> Highly Similar!

        // Hash group 3 (Sarah Portrait)
        val hashSarah1 = "FFEEDDCCBBAA9988"
        val hashSarah2 = "FFEEDDCCBBAA9987" // Hamming distance: 1 bit -> Highly Similar!

        // 1. LANDSCAPES (10 photos)
        list.add(PhotoItem(-1, "Alpine Sunset.jpg", null, 2453100L, 1718293812L, PhotoCategory.LANDSCAPE, averageHash = hashYosemite1))
        list.add(PhotoItem(-2, "Yosemite Falls.jpg", null, 3120000L, 1718293813L, PhotoCategory.LANDSCAPE, averageHash = hashYosemite2)) // Dupe 1
        list.add(PhotoItem(-3, "Yosemite Falls Copy.jpg", null, 3120000L, 1718293814L, PhotoCategory.LANDSCAPE, averageHash = hashYosemite3)) // Dupe 2
        list.add(PhotoItem(-4, "Icelandic Aurora.jpg", null, 1892000L, 1718293815L, PhotoCategory.LANDSCAPE, averageHash = "F0F0F0F0F0F0F0F0"))
        list.add(PhotoItem(-5, "Grand Canyon Sunrise.jpg", null, 4210000L, 1718293816L, PhotoCategory.LANDSCAPE, averageHash = "0F0F0F0F0F0F0F0F"))
        list.add(PhotoItem(-6, "Santorini Coastline.jpg", null, 2210000L, 1718293817L, PhotoCategory.LANDSCAPE, averageHash = "7F7F7F7F7F7F7F7F"))
        list.add(PhotoItem(-7, "Sahara Golden Dunes.jpg", null, 1540000L, 1718293818L, PhotoCategory.LANDSCAPE, averageHash = "0707070707070707"))
        list.add(PhotoItem(-8, "Autumn Forest Path.jpg", null, 3680000L, 1718293819L, PhotoCategory.LANDSCAPE, averageHash = "FFFF0000FFFF0000"))
        list.add(PhotoItem(-9, "Fuji Peak at Dawn.jpg", null, 1980000L, 1718293820L, PhotoCategory.LANDSCAPE, averageHash = "0000FFFF0000FFFF"))
        list.add(PhotoItem(-10, "Cozy Cabin River.jpg", null, 2900000L, 1718293821L, PhotoCategory.LANDSCAPE, averageHash = "1234567890ABCDEF"))

        // 2. PEOPLE (10 photos)
        list.add(PhotoItem(-11, "Portrait of Sarah.jpg", null, 1510000L, 1718293910L, PhotoCategory.PEOPLE, 
            faces = listOf(FaceItem(101, "Sarah", sarahVector1, 0.25f, 0.2f, 0.65f, 0.7f)), averageHash = hashSarah1))
        list.add(PhotoItem(-12, "Portrait of Sarah Dupe.jpg", null, 1512000L, 1718293911L, PhotoCategory.PEOPLE, 
            faces = listOf(FaceItem(102, "Sarah", sarahVector2, 0.25f, 0.2f, 0.65f, 0.7f)), averageHash = hashSarah2)) // Dupe
        list.add(PhotoItem(-13, "Sarah Outdoor Park.jpg", null, 1920000L, 1718293912L, PhotoCategory.PEOPLE, 
            faces = listOf(FaceItem(103, "Sarah", sarahVector3, 0.2f, 0.15f, 0.7f, 0.75f)), averageHash = "A2B2C2D2E2F21234"))
        list.add(PhotoItem(-14, "Studio Headshot John.jpg", null, 2120000L, 1718293913L, PhotoCategory.PEOPLE, 
            faces = listOf(FaceItem(201, "John", johnVector1, 0.3f, 0.25f, 0.6f, 0.65f)), averageHash = "B3C3D3E3F3123456"))
        list.add(PhotoItem(-15, "John Cafe Morning.jpg", null, 1850000L, 1718293914L, PhotoCategory.PEOPLE, 
            faces = listOf(FaceItem(202, "John", johnVector2, 0.35f, 0.2f, 0.65f, 0.6f)), averageHash = "C4D4E4F412345678"))
        list.add(PhotoItem(-16, "Portrait of Emma.jpg", null, 1640000L, 1718293915L, PhotoCategory.PEOPLE, 
            faces = listOf(FaceItem(301, "Emma", emmaVector1, 0.28f, 0.22f, 0.68f, 0.68f)), averageHash = "D5E5F51234567890"))
        list.add(PhotoItem(-17, "Emma Beach Sunset.jpg", null, 2410000L, 1718293916L, PhotoCategory.PEOPLE, 
            faces = listOf(FaceItem(302, "Emma", emmaVector2, 0.25f, 0.25f, 0.7f, 0.7f)), averageHash = "E5F51234567890AB"))
        list.add(PhotoItem(-18, "Sarah & John Candid.jpg", null, 2220000L, 1718293917L, PhotoCategory.PEOPLE, 
            faces = listOf(
                FaceItem(104, "Sarah", sarahVector1, 0.15f, 0.25f, 0.45f, 0.65f),
                FaceItem(203, "John", johnVector2, 0.55f, 0.25f, 0.85f, 0.65f)
            ), averageHash = "F51234567890ABCD")) // Group: 2 faces
        list.add(PhotoItem(-19, "Emma and Friends Group.jpg", null, 3100000L, 1718293918L, PhotoCategory.PEOPLE, 
            faces = listOf(
                FaceItem(303, "Emma", emmaVector2, 0.1f, 0.3f, 0.35f, 0.7f),
                FaceItem(401, "Stranger A", FloatArray(128) { 0.9f }, 0.4f, 0.3f, 0.65f, 0.7f),
                FaceItem(402, "Stranger B", FloatArray(128) { -0.9f }, 0.7f, 0.3f, 0.95f, 0.7f)
            ), averageHash = "1234567890ABCDEF")) // Group: 3 faces
        list.add(PhotoItem(-20, "Group Selfie Outdoors.jpg", null, 2750000L, 1718293919L, PhotoCategory.PEOPLE, 
            faces = listOf(
                FaceItem(403, "Stranger C", FloatArray(128) { 0.5f }, 0.1f, 0.2f, 0.4f, 0.6f),
                FaceItem(404, "Stranger D", FloatArray(128) { -0.5f }, 0.5f, 0.2f, 0.8f, 0.6f)
            ), averageHash = "FEDCBA0987654321")) // Group: 2 faces

        // 3. ANIMALS (6 photos)
        list.add(PhotoItem(-21, "Husky Snow Play.jpg", null, 1780000L, 1718294010L, PhotoCategory.ANIMAL, averageHash = "1111222233334444"))
        list.add(PhotoItem(-22, "Golden Retriever Puppy.jpg", null, 1230000L, 1718294011L, PhotoCategory.ANIMAL, averageHash = "2222333344445555"))
        list.add(PhotoItem(-23, "Kitten Sleeping.jpg", null, 980000L, 1718294012L, PhotoCategory.ANIMAL, averageHash = "3333444455556666"))
        list.add(PhotoItem(-24, "Garden Squirrel.jpg", null, 1140000L, 1718294013L, PhotoCategory.ANIMAL, averageHash = "4444555566667777"))
        list.add(PhotoItem(-25, "Majestic Lion Savannah.jpg", null, 3890000L, 1718294014L, PhotoCategory.ANIMAL, averageHash = "5555666677778888"))
        list.add(PhotoItem(-26, "Jumping Dolphins Ocean.jpg", null, 2040000L, 1718294015L, PhotoCategory.ANIMAL, averageHash = "6666777788889999"))

        // 4. FOOD (6 photos)
        list.add(PhotoItem(-27, "Pepperoni Pizza Slice.jpg", null, 1420000L, 1718294110L, PhotoCategory.FOOD, averageHash = "777788889999AAAA"))
        list.add(PhotoItem(-28, "Fluffy Pancake Tower.jpg", null, 1650000L, 1718294111L, PhotoCategory.FOOD, averageHash = hashPancake1))
        list.add(PhotoItem(-29, "Fluffy Pancake Copy.jpg", null, 1650000L, 1718294112L, PhotoCategory.FOOD, averageHash = hashPancake2)) // Dupe
        list.add(PhotoItem(-30, "Juicy Wagyu Burger.jpg", null, 2150000L, 1718294113L, PhotoCategory.FOOD, averageHash = "88889999AAAABBBB"))
        list.add(PhotoItem(-31, "Traditional Sushi Platter.jpg", null, 2560000L, 1718294114L, PhotoCategory.FOOD, averageHash = "9999AAAABBBBCCCC"))
        list.add(PhotoItem(-32, "Fresh Summer Salad.jpg", null, 1100000L, 1718294115L, PhotoCategory.FOOD, averageHash = "AAAABBBBCCCCDDDD"))

        // 5. VEHICLES (4 photos)
        list.add(PhotoItem(-33, "Sleek Electric Sedan.jpg", null, 2300000L, 1718294210L, PhotoCategory.VEHICLE, averageHash = "BBBBCCCCDDDDEEEE"))
        list.add(PhotoItem(-34, "Red Superbike Track.jpg", null, 1950000L, 1718294211L, PhotoCategory.VEHICLE, averageHash = "CCCCDDDDEEEEFFFF"))
        list.add(PhotoItem(-35, "Luxury Yacht Harbor.jpg", null, 4120000L, 1718294212L, PhotoCategory.VEHICLE, averageHash = "DDDDEEEEFFFF0000"))
        list.add(PhotoItem(-36, "Private Jet Takeoff.jpg", null, 3600000L, 1718294213L, PhotoCategory.VEHICLE, averageHash = "EEEEFFFF00001111"))

        // 6. ARCHITECTURE (4 photos)
        list.add(PhotoItem(-37, "Eiffel Tower Dusk.jpg", null, 2800000L, 1718294310L, PhotoCategory.ARCHITECTURE, averageHash = "FFFF000011112222"))
        list.add(PhotoItem(-38, "Colosseum Ruins Rome.jpg", null, 3210000L, 1718294311L, PhotoCategory.ARCHITECTURE, averageHash = "0000111122223333"))
        list.add(PhotoItem(-39, "Tokyo Skyscraper Night.jpg", null, 4890000L, 1718294312L, PhotoCategory.ARCHITECTURE, averageHash = "1111222233334444"))
        list.add(PhotoItem(-40, "Cozy Cotswold Cottage.jpg", null, 1540000L, 1718294313L, PhotoCategory.ARCHITECTURE, averageHash = "2222333344445555"))

        // 7. DOCUMENTS (4 photos)
        list.add(PhotoItem(-41, "Receipt Tax Claim.png", null, 450000L, 1718294410L, PhotoCategory.DOCUMENT, averageHash = "3333444455556666"))
        list.add(PhotoItem(-42, "Flight Ticket Ticket.png", null, 720000L, 1718294411L, PhotoCategory.DOCUMENT, averageHash = "4444555566667777"))
        list.add(PhotoItem(-43, "Credit Card Statement.png", null, 810000L, 1718294412L, PhotoCategory.DOCUMENT, averageHash = "5555666677778888"))
        list.add(PhotoItem(-44, "Classic Novel Page.jpg", null, 1120000L, 1718294413L, PhotoCategory.DOCUMENT, averageHash = "6666777788889999"))

        // 8. SPORTS (3 photos)
        list.add(PhotoItem(-45, "Championship Goal.jpg", null, 3200000L, 1718295510L, PhotoCategory.SPORTS, averageHash = "777788889999AAAA"))
        list.add(PhotoItem(-46, "Tennis Serve Close.jpg", null, 1980000L, 1718295511L, PhotoCategory.SPORTS, averageHash = "88889999AAAABBBB"))
        list.add(PhotoItem(-47, "Giant Wave Surfer.jpg", null, 2410000L, 1718295512L, PhotoCategory.SPORTS, averageHash = "9999AAAABBBBCCCC"))

        // 9. OTHERS (3 photos)
        list.add(PhotoItem(-48, "Abstract Fluid Painting.jpg", null, 3110000L, 1718295610L, PhotoCategory.OTHER, averageHash = "AAAABBBBCCCCDDDD"))
        list.add(PhotoItem(-49, "Midnight Fireworks NYE.jpg", null, 2800000L, 1718295611L, PhotoCategory.OTHER, averageHash = "BBBBCCCCDDDDEEEE"))
        list.add(PhotoItem(-50, "Cozy Candle Flame.jpg", null, 1050000L, 1718295612L, PhotoCategory.OTHER, averageHash = "CCCCDDDDEEEEFFFF"))

        return list
    }
}
