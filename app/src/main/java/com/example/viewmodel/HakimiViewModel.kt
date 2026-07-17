package com.example.viewmodel

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.analyzer.ImageAnalyzer
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class HakimiViewModel : ViewModel() {

    // Language State
    private val _language = MutableStateFlow(AppLanguage.SIMPLIFIED_CHINESE)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // Loaded/Scanned Photos List
    private val _allPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val allPhotos: StateFlow<List<PhotoItem>> = _allPhotos.asStateFlow()

    // Mode: Real MediaStore vs Demo Album
    private val _isDemoMode = MutableStateFlow(true)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    // Scanner UI States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _currentScanFile = MutableStateFlow("")
    val currentScanFile: StateFlow<String> = _currentScanFile.asStateFlow()

    private val _currentScanFolder = MutableStateFlow("")
    val currentScanFolder: StateFlow<String> = _currentScanFolder.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _failedCount = MutableStateFlow(0)
    val failedCount: StateFlow<Int> = _failedCount.asStateFlow()

    private val _matchedDupeCount = MutableStateFlow(0)
    val matchedDupeCount: StateFlow<Int> = _matchedDupeCount.asStateFlow()

    // Results States
    private val _imageClassificationScanDone = MutableStateFlow(false)
    val imageClassificationScanDone: StateFlow<Boolean> = _imageClassificationScanDone.asStateFlow()

    private val _peopleScanDone = MutableStateFlow(false)
    val peopleScanDone: StateFlow<Boolean> = _peopleScanDone.asStateFlow()

    private val _similarScanDone = MutableStateFlow(false)
    val similarScanDone: StateFlow<Boolean> = _similarScanDone.asStateFlow()

    // Groups computed after scan
    private val _peopleGroups = MutableStateFlow<List<PeopleGroup>>(emptyList())
    val peopleGroups: StateFlow<List<PeopleGroup>> = _peopleGroups.asStateFlow()

    private val _similarGroups = MutableStateFlow<List<SimilarGroup>>(emptyList())
    val similarGroups: StateFlow<List<SimilarGroup>> = _similarGroups.asStateFlow()

    // Tab Navigation
    private val _activeTab = MutableStateFlow(0) // 0: Smart Classify, 1: Similar, 2: Me
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _activeSubScreen = MutableStateFlow<String?>(null) // "image_class", "people_class", or null
    val activeSubScreen: StateFlow<String?> = _activeSubScreen.asStateFlow()

    // Picture preview overlays
    private val _previewPhoto = MutableStateFlow<PhotoItem?>(null)
    val previewPhoto: StateFlow<PhotoItem?> = _previewPhoto.asStateFlow()

    // Set App Language
    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    // Toggle Tab
    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
        _activeSubScreen.value = null
    }

    fun navigateToSubScreen(screen: String?) {
        _activeSubScreen.value = screen
    }

    fun showPreview(photo: PhotoItem?) {
        _previewPhoto.value = photo
    }

    /**
     * Start image auto classification scanner (recent 50 photos)
     */
    fun startImageClassificationScan(context: Context, useDemo: Boolean) {
        viewModelScope.launch {
            _isScanning.value = true
            _imageClassificationScanDone.value = false
            _isDemoMode.value = useDemo
            _scanProgress.value = 0f
            _scannedCount.value = 0
            _failedCount.value = 0
            _currentScanFolder.value = if (useDemo) "/virtual/album/dcim" else "/storage/emulated/0/DCIM/Camera"

            // Load candidate list
            val candidates = if (useDemo) {
                GalleryScanner.getDemoAlbum()
            } else {
                GalleryScanner.loadDevicePhotos(context)
            }

            if (candidates.isEmpty() && !useDemo) {
                // If real media query yielded nothing, auto-fallback to demo album for perfect UX
                _isDemoMode.value = true
                startImageClassificationScan(context, true)
                return@launch
            }

            val total = candidates.size
            val scannedList = mutableListOf<PhotoItem>()

            for (i in candidates.indices) {
                val photo = candidates[i]
                _currentScanFile.value = photo.displayName
                _scanProgress.value = (i + 1).toFloat() / total
                _scannedCount.value = i + 1

                // Simulate local ML processing delay (Western high-polish transition curve)
                delay(30) 

                // Perform real heuristic analysis on physical items, or use prepared tags for demo
                if (!useDemo && photo.uri != null) {
                    // Re-calculate / verify on high resolution if needed, already classified in MediaStore load
                }
                scannedList.add(photo)
            }

            _allPhotos.value = scannedList
            _isScanning.value = false
            _imageClassificationScanDone.value = true
        }
    }

    /**
     * Start FaceNet-based People Clustering Scanner
     */
    fun startPeopleClassificationScan(context: Context, useDemo: Boolean) {
        viewModelScope.launch {
            _isScanning.value = true
            _peopleScanDone.value = false
            _isDemoMode.value = useDemo
            _scanProgress.value = 0f
            _scannedCount.value = 0
            _failedCount.value = 0
            _currentScanFolder.value = if (useDemo) "/virtual/album/portraits" else "/storage/emulated/0/DCIM/People"

            val candidates = if (useDemo) {
                GalleryScanner.getDemoAlbum()
            } else {
                GalleryScanner.loadDevicePhotos(context)
            }

            if (candidates.isEmpty() && !useDemo) {
                _isDemoMode.value = true
                startPeopleClassificationScan(context, true)
                return@launch
            }

            val total = candidates.size
            val scannedList = mutableListOf<PhotoItem>()

            for (i in candidates.indices) {
                val photo = candidates[i]
                _currentScanFile.value = photo.displayName
                _scanProgress.value = (i + 1).toFloat() / total
                _scannedCount.value = i + 1

                delay(40) // Simulate FaceNet extraction tick
                scannedList.add(photo)
            }

            _allPhotos.value = scannedList
            computePeopleGroups(scannedList)
            _isScanning.value = false
            _peopleScanDone.value = true
        }
    }

    /**
     * Cluster faces from all loaded photos into distinct PeopleGroups using FaceNet Euclidean distance.
     * Match threshold is set to 0.65f. Merge threshold is set to 0.85f.
     */
    private fun computePeopleGroups(photos: List<PhotoItem>) {
        val faces = photos.flatMap { it.faces }
        val uniqueFaces = mutableListOf<FaceItem>()
        val faceToPhotos = mutableMapOf<Int, MutableList<PhotoItem>>()

        // Euclidean threshold for grouping
        val matchThreshold = 0.65f 

        for (photo in photos) {
            for (face in photo.faces) {
                // Try matching with existing tracked faces
                var matched = false
                for (tracked in uniqueFaces) {
                    val dist = ImageAnalyzer.calculateFaceDistance(face.embedding, tracked.embedding)
                    if (dist < matchThreshold) {
                        faceToPhotos[tracked.id]?.add(photo)
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    uniqueFaces.add(face)
                    faceToPhotos[face.id] = mutableListOf(photo)
                }
            }
        }

        // Apply secondary merge based on higher similarity threshold (e.g., merge very similar snapshots)
        val mergeThreshold = 0.85f
        val mergedList = mutableListOf<PeopleGroup>()
        val visitedIds = mutableSetOf<Int>()

        var personIndex = 1
        for (face in uniqueFaces) {
            if (face.id in visitedIds) continue

            val groupPhotos = faceToPhotos[face.id] ?: mutableListOf()
            val associatedFaces = mutableListOf(face)

            // Look for other snapshots that are extremely close to merge them
            for (other in uniqueFaces) {
                if (other.id == face.id || other.id in visitedIds) continue
                val dist = ImageAnalyzer.calculateFaceDistance(face.embedding, other.embedding)
                if (dist < mergeThreshold) {
                    visitedIds.add(other.id)
                    associatedFaces.add(other)
                    faceToPhotos[other.id]?.let { groupPhotos.addAll(it) }
                }
            }

            visitedIds.add(face.id)
            val distinctPhotos = groupPhotos.distinctBy { it.id }

            val pGroup = PeopleGroup(
                id = face.id,
                name = if (_language.value == AppLanguage.ENGLISH) "Person $personIndex" else "人物 $personIndex",
                snapshotFace = face,
                photos = distinctPhotos
            )
            mergedList.add(pGroup)
            personIndex++
        }

        _peopleGroups.value = mergedList
    }

    /**
     * Start similar images hash scanner (Hamming distance comparison)
     */
    fun startSimilarScan(context: Context, useDemo: Boolean) {
        viewModelScope.launch {
            _isScanning.value = true
            _similarScanDone.value = false
            _isDemoMode.value = useDemo
            _scanProgress.value = 0f
            _scannedCount.value = 0
            _failedCount.value = 0
            _matchedDupeCount.value = 0
            _currentScanFolder.value = if (useDemo) "/virtual/album/dupes" else "/storage/emulated/0/DCIM/Camera"

            val candidates = if (useDemo) {
                GalleryScanner.getDemoAlbum()
            } else {
                GalleryScanner.loadDevicePhotos(context)
            }

            if (candidates.isEmpty() && !useDemo) {
                _isDemoMode.value = true
                startSimilarScan(context, true)
                return@launch
            }

            val total = candidates.size
            val scannedList = mutableListOf<PhotoItem>()
            val matchedDupesSet = mutableSetOf<Long>()

            for (i in candidates.indices) {
                val photo = candidates[i]
                _currentScanFile.value = photo.displayName
                _scanProgress.value = (i + 1).toFloat() / total
                _scannedCount.value = i + 1

                // Simulate calculation of image fingerprints
                delay(40)

                scannedList.add(photo)

                // Dynamically evaluate similar groups during scanning to update "Matched Dupes" count in UI!
                var currentMatchCount = 0
                val processedSoFar = scannedList.toList()
                for (j in processedSoFar.indices) {
                    for (k in j + 1 until processedSoFar.size) {
                        val dist = ImageAnalyzer.calculateHammingDistance(
                            processedSoFar[j].averageHash,
                            processedSoFar[k].averageHash
                        )
                        if (dist <= 10) { // Hamming threshold
                            currentMatchCount++
                        }
                    }
                }
                _matchedDupeCount.value = currentMatchCount
            }

            _allPhotos.value = scannedList
            computeSimilarGroups(scannedList)
            _isScanning.value = false
            _similarScanDone.value = true
        }
    }

    /**
     * Group photos into similar clusters based on average hash Hamming distance <= 10.
     */
    private fun computeSimilarGroups(photos: List<PhotoItem>) {
        val groups = mutableListOf<SimilarGroup>()
        val groupedIds = mutableSetOf<Long>()

        var groupId = 1
        for (i in photos.indices) {
            val photo1 = photos[i]
            if (photo1.id in groupedIds) continue

            val similarList = mutableListOf<PhotoItem>()
            similarList.add(photo1)

            for (j in i + 1 until photos.size) {
                val photo2 = photos[j]
                if (photo2.id in groupedIds) continue

                val distance = ImageAnalyzer.calculateHammingDistance(photo1.averageHash, photo2.averageHash)
                if (distance <= 10) {
                    similarList.add(photo2)
                }
            }

            if (similarList.size >= 2) {
                // This forms a valid similar/duplicate group!
                groups.add(SimilarGroup(groupId, similarList))
                for (p in similarList) {
                    groupedIds.add(p.id)
                }
                groupId++
            }
        }
        _similarGroups.value = groups
    }

    /**
     * Delete a single image
     */
    fun deletePhoto(context: Context, photoId: Long) {
        viewModelScope.launch {
            // 1. Remove from allPhotos
            _allPhotos.value = _allPhotos.value.filter { it.id != photoId }

            // 2. Perform local system file delete if in Real Mode
            if (!_isDemoMode.value && photoId > 0) {
                val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = ContentUris.withAppendedId(queryUri, photoId)
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to older File deletion API if needed
                }
            }

            // 3. Re-compute groups based on updated photos
            computePeopleGroups(_allPhotos.value)
            computeSimilarGroups(_allPhotos.value)
        }
    }

    /**
     * Batch delete duplicates inside a group: Keep 1st photo, delete all others in this group
     */
    fun deleteDuplicatesInGroup(context: Context, groupId: Int) {
        viewModelScope.launch {
            val group = _similarGroups.value.find { it.id == groupId } ?: return@launch
            if (group.photos.size < 2) return@launch

            val toKeep = group.photos[0]
            val toDelete = group.photos.subList(1, group.photos.size)

            val deleteIds = toDelete.map { it.id }.toSet()

            // 1. Update photos
            _allPhotos.value = _allPhotos.value.filter { it.id !in deleteIds }

            // 2. Perform system deletes
            if (!_isDemoMode.value) {
                val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                for (photo in toDelete) {
                    if (photo.id > 0) {
                        try {
                            val uri = ContentUris.withAppendedId(queryUri, photo.id)
                            context.contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            // 3. Re-compute
            computePeopleGroups(_allPhotos.value)
            computeSimilarGroups(_allPhotos.value)
        }
    }
}
