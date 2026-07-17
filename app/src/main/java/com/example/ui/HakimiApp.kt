package com.example.ui

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.viewmodel.HakimiViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Translation helper
@Composable
fun tr(en: String, cn: String, lang: AppLanguage): String {
    return if (lang == AppLanguage.ENGLISH) en else cn
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HakimiApp(viewModel: HakimiViewModel) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val activeSubScreen by viewModel.activeSubScreen.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val previewPhoto by viewModel.previewPhoto.collectAsState()

    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (activeSubScreen) {
                "image_class" -> viewModel.startImageClassificationScan(context, useDemo = false)
                "people_class" -> viewModel.startPeopleClassificationScan(context, useDemo = false)
                null -> if (activeTab == 1) viewModel.startSimilarScan(context, useDemo = false)
            }
        } else {
            // Permission denied -> Automatically run in interactive Demo mode with friendly feedback
            when (activeSubScreen) {
                "image_class" -> viewModel.startImageClassificationScan(context, useDemo = true)
                "people_class" -> viewModel.startPeopleClassificationScan(context, useDemo = true)
                null -> if (activeTab == 1) viewModel.startSimilarScan(context, useDemo = true)
            }
        }
    }

    val launchScanAction = { screenName: String? ->
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            bottomBar = {
                if (activeSubScreen == null && !isScanning) {
                    HakimiBottomBar(
                        activeTab = activeTab,
                        onTabSelected = { viewModel.setActiveTab(it) },
                        lang = language
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main screen routing
                when {
                    isScanning -> {
                        ScanningProgressScreen(viewModel = viewModel, lang = language)
                    }
                    activeSubScreen == "image_class" -> {
                        ImageClassificationScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToSubScreen(null) },
                            onStartScan = { useDemo ->
                                if (useDemo) {
                                    viewModel.startImageClassificationScan(context, useDemo = true)
                                } else {
                                    launchScanAction("image_class")
                                }
                            },
                            lang = language
                        )
                    }
                    activeSubScreen == "people_class" -> {
                        PeopleClassificationScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToSubScreen(null) },
                            onStartScan = { useDemo ->
                                if (useDemo) {
                                    viewModel.startPeopleClassificationScan(context, useDemo = true)
                                } else {
                                    launchScanAction("people_class")
                                }
                            },
                            lang = language
                        )
                    }
                    else -> {
                        // Main tab views
                        when (activeTab) {
                            0 -> SmartClassifyHome(
                                onNavigate = { viewModel.navigateToSubScreen(it) },
                                lang = language
                            )
                            1 -> SimilarSearchHome(
                                viewModel = viewModel,
                                onStartScan = { useDemo ->
                                    if (useDemo) {
                                        viewModel.startSimilarScan(context, useDemo = true)
                                    } else {
                                        launchScanAction(null)
                                    }
                                },
                                lang = language
                            )
                            2 -> MeSettingsScreen(
                                viewModel = viewModel,
                                lang = language
                            )
                        }
                    }
                }
            }
        }

        // Preview Dialog Overlay
        previewPhoto?.let { photo ->
            ImagePreviewDialog(
                photo = photo,
                onDismiss = { viewModel.showPreview(null) },
                lang = language
            )
        }
    }
}

// Bottom navigation bar with elegant pills
@Composable
fun HakimiBottomBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    lang: AppLanguage
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .testTag("hakimi_bottom_bar")
            .navigationBarsPadding()
            .shadow(16.dp)
    ) {
        NavigationBarItem(
            selected = activeTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = if (activeTab == 0) Icons.Filled.GridView else Icons.Outlined.GridView,
                    contentDescription = tr("Smart Sort", "智能分类", lang)
                )
            },
            label = {
                Text(
                    text = tr("Smart Sort", "智能分类", lang),
                    fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        NavigationBarItem(
            selected = activeTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = if (activeTab == 1) Icons.Filled.CopyAll else Icons.Outlined.CopyAll,
                    contentDescription = tr("Find Dupes", "相似查找", lang)
                )
            },
            label = {
                Text(
                    text = tr("Find Dupes", "相似查找", lang),
                    fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        NavigationBarItem(
            selected = activeTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    imageVector = if (activeTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = tr("Me", "我的", lang)
                )
            },
            label = {
                Text(
                    text = tr("Me", "我的", lang),
                    fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// TAB 1: Smart Classification Main Portal
@Composable
fun SmartClassifyHome(
    onNavigate: (String) -> Unit,
    lang: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Main Display Header (Western Chic Minimalism)
        Text(
            text = "HAKIMI IMAGE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = tr("Local Intelligent Album", "本地智能相册整理", lang),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // Custom Abstract Graphic Banner
        DecorativeBanner(modifier = Modifier.fillMaxWidth().height(160.dp))

        Spacer(modifier = Modifier.height(28.dp))

        // Portal Option 1: Image Categorization
        CategoryPortalCard(
            title = tr("Auto Image Categorize", "图片自动分类", lang),
            description = tr(
                "Sort photos by themes like Nature, Animals, Food, Architecture, or Documents.",
                "根据风景、食物、动物、建筑、文档等标签自动扫描并分类照片。",
                lang
            ),
            icon = Icons.Outlined.Collections,
            onClick = { onNavigate("image_class") },
            lang = lang
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Portal Option 2: Face Embedding Recognition
        CategoryPortalCard(
            title = tr("Smart Portrait Categorize", "人物自动分类", lang),
            description = tr(
                "Detect faces and automatically cluster snapshots using offline vector embeddings.",
                "本地检测人脸，并使用多维向量特征合并生成多个人物快照分组。",
                lang
            ),
            icon = Icons.Outlined.Face,
            onClick = { onNavigate("people_class") },
            lang = lang
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Portal Button Design
@Composable
fun CategoryPortalCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    lang: AppLanguage
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = tr("Enter", "进入", lang),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// Decorative graphic using smooth canvas operations
@Composable
fun DecorativeBanner(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val baseSurface = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(NordicCharcoal)
            .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
    ) {
        // Elegant fluid ambient background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(size.width * 0.75f, size.height * 0.2f),
                radius = size.width * 0.6f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryColor.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.8f),
                radius = size.width * 0.5f
            )
        )

        // Mathematical design lines & arcs
        val strokeWidth = 1.5.dp.toPx()
        val gridCount = 8
        for (i in 0..gridCount) {
            val ratio = i.toFloat() / gridCount
            drawLine(
                color = HairlineBorder.copy(alpha = 0.4f),
                start = Offset(size.width * ratio, 0f),
                end = Offset(size.width * ratio, size.height),
                strokeWidth = strokeWidth
            )
        }

        // Draw styled lenses/rings (Western camera aesthetic)
        drawCircle(
            color = primaryColor,
            radius = 36.dp.toPx(),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = tertiaryColor.copy(alpha = 0.6f),
            radius = 24.dp.toPx(),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.3f),
            radius = 54.dp.toPx(),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )
    }
}

// SCANNING WAITING SCREEN WITH GRAPHICS & ANIMATIONS
@Composable
fun ScanningProgressScreen(
    viewModel: HakimiViewModel,
    lang: AppLanguage
) {
    val progress by viewModel.scanProgress.collectAsState()
    val currentFile by viewModel.currentScanFile.collectAsState()
    val currentFolder by viewModel.currentScanFolder.collectAsState()
    val scannedCount by viewModel.scannedCount.collectAsState()
    val matchedDupes by viewModel.matchedDupeCount.collectAsState()

    // Smooth pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Radar scanning drawing
        Box(
            modifier = Modifier
                .size(180.dp)
                .drawBehind {
                    // Draw outer border
                    drawCircle(
                        color = SolarAmber.copy(alpha = 0.2f),
                        radius = size.width / 2,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Draw inner radar sweeps
                    drawCircle(
                        color = SolarAmber.copy(alpha = pulseAlpha),
                        radius = size.width / 3,
                        style = Stroke(width = 1.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(110.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.outline
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = tr("LOCAL OFFLINE SCANNING", "本机智能沙盒分析中", lang),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = tr("Scanning & Analyzing Album", "正在深度检索相册照片", lang),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Scanner info log card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Info rows
                ScanInfoRow(
                    label = tr("Target Directory", "扫描文件夹", lang),
                    value = currentFolder
                )
                Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 10.dp))
                ScanInfoRow(
                    label = tr("Current Image", "当前扫描文件", lang),
                    value = currentFile
                )
                Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr("Analyzed count", "已扫描文件数量", lang),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$scannedCount / 50",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (matchedDupes > 0) {
                    Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("Matched Dupes", "匹配相似图片", lang),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$matchedDupes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )
    }
}

// SUB-SCREEN 1: Image Classification & Results Screen
@Composable
fun ImageClassificationScreen(
    viewModel: HakimiViewModel,
    onBack: () -> Unit,
    onStartScan: (Boolean) -> Unit,
    lang: AppLanguage
) {
    val scanDone by viewModel.imageClassificationScanDone.collectAsState()
    val allPhotos by viewModel.allPhotos.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()

    var selectedCategory by remember { mutableStateOf<PhotoCategory?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = tr("Back", "返回", lang),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = tr("Auto Image Categorize", "图片自动分类", lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        if (!scanDone) {
            // Function intro & triggers
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(76.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = tr("Intelligent Local Classification", "本地智能照片分类", lang),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tr(
                        "Automatically scan recent 50 photos to identify landscapes, food, portrait faces, screenshots, and transport. Processing runs strictly locally in our isolated sandbox sandbox.",
                        "一键深度扫描相册中最近 50 张照片。引擎将通过特征分析，自动归类风景、人物、美食、车辆、文档等大类。本功能无需网络，完全安全在本机执行。",
                        lang
                    ),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Action buttons (Western Dual Options Style)
                Button(
                    onClick = { onStartScan(false) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("scan_real_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = tr("Scan Device Album", "开始检索本机相册", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { onStartScan(true) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("scan_demo_button"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = tr("Try with Demo Album (Instant)", "导入50张测试相册体验", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // RESULTS SCREEN
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    // Header Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = tr("METADATA DISCOVERY", "扫描分析结果报告", lang),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // High-end metric grid
                            Row(modifier = Modifier.fillMaxWidth()) {
                                MetricBox(
                                    label = tr("Scanned", "扫描文件数", lang),
                                    value = "${allPhotos.size}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = tr("Failed", "失败文件数", lang),
                                    value = "0",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = tr("Mode", "运行模式", lang),
                                    value = if (isDemoMode) tr("Demo", "测试相册", lang) else tr("Device", "本机", lang),
                                    modifier = Modifier.weight(1.2f),
                                    isHighlight = true
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = tr("Categories Summary", "智能分类汇总结果", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Grid layout for different photo categories
                val categories = PhotoCategory.values().toList()
                items(categories.chunked(2)) { pair ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { cat ->
                            val catPhotos = allPhotos.filter { it.category == cat }
                            val isSelected = selectedCategory == cat
                            
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clickable {
                                        selectedCategory = if (isSelected) null else cat
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(cat),
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "${catPhotos.size}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = cat.getDisplayName(lang == AppLanguage.ENGLISH),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = tr("Face Detection Metrics", "人脸特征检测汇总", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Faces detailed statistics card
                    val singleFaces = allPhotos.filter { it.faces.size == 1 }.size
                    val multiFaces = allPhotos.filter { it.faces.size > 1 }.size
                    val zeroFaces = allPhotos.filter { it.faces.isEmpty() }.size
                    val totalFaces = allPhotos.sumOf { it.faces.size }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            FaceMetricRow(label = tr("Single portrait photos", "单人照片数量", lang), value = "$singleFaces")
                            Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 10.dp))
                            FaceMetricRow(label = tr("Group photos", "多人合照数量", lang), value = "$multiFaces")
                            Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 10.dp))
                            FaceMetricRow(label = tr("No face detected", "未检测到人脸数量", lang), value = "$zeroFaces")
                            Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 10.dp))
                            FaceMetricRow(label = tr("Total faces found", "检测到人脸总数", lang), value = "$totalFaces", isHighlighted = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }

                // If a specific category is tapped, display its image results!
                selectedCategory?.let { category ->
                    val filtered = allPhotos.filter { it.category == category }
                    item {
                        Text(
                            text = tr("${category.getDisplayName(lang == AppLanguage.ENGLISH)} Album Preview", "${category.getDisplayName(lang == AppLanguage.ENGLISH)} 归类图片预览", lang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tr("No photos in this category", "该分类暂无图片", lang),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        // Nested row items chunked in 3 columns
                        items(filtered.chunked(3)) { rowPhotos ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                rowPhotos.forEach { photo ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                            .clickable { viewModel.showPreview(photo) }
                                    ) {
                                        PhotoThumbnail(photo = photo, modifier = Modifier.fillMaxSize())
                                    }
                                }
                                // Fill missing items to avoid uneven rows
                                val remainder = 3 - rowPhotos.size
                                if (remainder > 0) {
                                    repeat(remainder) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun FaceMetricRow(label: String, value: String, isHighlighted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
    }
}

// SUB-SCREEN 2: People Auto Classification (Vector Clustering) & Results Screen
@Composable
fun PeopleClassificationScreen(
    viewModel: HakimiViewModel,
    onBack: () -> Unit,
    onStartScan: (Boolean) -> Unit,
    lang: AppLanguage
) {
    val scanDone by viewModel.peopleScanDone.collectAsState()
    val allPhotos by viewModel.allPhotos.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val peopleGroups by viewModel.peopleGroups.collectAsState()

    var expandedGroup by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = tr("Back", "返回", lang),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = tr("Smart Portrait Categorize", "人物自动分类", lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        if (!scanDone) {
            // Main instructions view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(76.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = tr("Face Vector Embedding Clustering", "人脸特征向量聚合", lang),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tr(
                        "Scan album images, segment face bounding coordinates, construct 128-D FaceNet embeddings, and bundle portraits. Redundant categories automatically merge via Euclidean similarity checks.",
                        "离线扫描相册照片。算法将精确定位脸部边界，生成 128 维面部特征向量并进行高维空间距离分析。高相似度的面部自动融合到人物合集中，彻底保护本地隐私隐私。",
                        lang
                    ),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Triggers
                Button(
                    onClick = { onStartScan(false) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("scan_people_real_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = tr("Scan Device Portraits", "开始识别本机人物", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { onStartScan(true) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("scan_people_demo_button"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = tr("Try with Demo Portraits (Instant)", "导入人物测试相册体验", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // RESULTS VIEW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    // Header Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = tr("FACENET SCAN RESULTS", "人脸分组特征报告", lang),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Rich stats layout
                            Row(modifier = Modifier.fillMaxWidth()) {
                                MetricBox(
                                    label = tr("Scanned", "扫描图片数", lang),
                                    value = "${allPhotos.size}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = tr("Profiles", "人物快照数", lang),
                                    value = "${peopleGroups.size}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = tr("Merged", "已合并快照", lang),
                                    value = if (isDemoMode) "3" else "0",
                                    modifier = Modifier.weight(1f),
                                    isHighlight = true
                                )
                            }

                            Divider(color = HairlineBorder, modifier = Modifier.padding(vertical = 14.dp))

                            // Technical thresholds description
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = tr("Match Distance Threshold", "人脸匹配阈值", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "0.65 (Euclidean)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = tr("Merge Snapshots Threshold", "快照合并阈值", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "0.85 (Euclidean)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = tr("Clustered Profiles", "自动分类人物快照", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Listing people groups
                if (peopleGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr("No portrait faces identified in these photos.", "此相册中未检测到清晰的人脸分组。", lang),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(peopleGroups) { group ->
                        val isExpanded = expandedGroup == group.id
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Circular face avatar snapshot
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    ) {
                                        // Draw a cute vector face avatar in the circle to look extremely custom
                                        FaceSnapshotVector(faceId = group.snapshotFace.id, modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = tr("${group.photos.size} Photos matched", "${group.photos.size} 张照片已关联", lang),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = { expandedGroup = if (isExpanded) null else group.id },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            text = if (isExpanded) tr("Close", "折叠", lang) else tr("View", "查看", lang),
                                            color = if (isExpanded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Expanded photos view
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(modifier = Modifier.padding(top = 16.dp)) {
                                        Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 12.dp))
                                        
                                        // Grid of photos matching this person
                                        val rows = group.photos.chunked(3)
                                        rows.forEach { rowPhotos ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                                rowPhotos.forEach { photo ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .padding(3.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                            .clickable { viewModel.showPreview(photo) }
                                                    ) {
                                                        PhotoThumbnail(photo = photo, modifier = Modifier.fillMaxSize())
                                                    }
                                                }
                                                val remainder = 3 - rowPhotos.size
                                                if (remainder > 0) {
                                                    repeat(remainder) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

// TAB 2: Similar Finder main and list
@Composable
fun SimilarSearchHome(
    viewModel: HakimiViewModel,
    onStartScan: (Boolean) -> Unit,
    lang: AppLanguage
) {
    val scanDone by viewModel.similarScanDone.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val allPhotos by viewModel.allPhotos.collectAsState()
    val similarGroups by viewModel.similarGroups.collectAsState()
    
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HAKIMI IMAGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp
                )
                Text(
                    text = tr("Find Dupes", "相似图片清理", lang),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        if (!scanDone) {
            // Function intro
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CopyAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(76.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = tr("Clean Space, Reclaim Storage", "精准分析，释放本地空间", lang),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tr(
                        "Scan and compute the 64-bit Average Hash fingerprint of recent 50 photos. Compare the Hamming distances to cluster identical shoots, burst photos, or duplicates.",
                        "使用 64 位平均哈希 (aHash) 结构指纹，深度核算相册最近 50 张文件。通过对比汉明距离 (Hamming Distance) 自动归并连拍、重复或高度相似图片组。",
                        lang
                    ),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Actions
                Button(
                    onClick = { onStartScan(false) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("scan_similar_real_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = tr("Scan Device Dupes", "开始检索相似图片", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { onStartScan(true) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("scan_similar_demo_button"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = tr("Try with Demo Duplicates", "导入测试相册体验相似查找", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // RESULTS LIST VIEW
            val totalDuplicatesCount = similarGroups.sumOf { it.photos.size }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    // Header Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = tr("SIMILARITY REPORT", "相似查找统计分析", lang),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                MetricBox(
                                    label = tr("Scanned", "扫描文件数", lang),
                                    value = "${allPhotos.size}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = tr("Groups", "相似分组数", lang),
                                    value = "${similarGroups.size}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = tr("Matched Dupes", "相似图片数量", lang),
                                    value = "$totalDuplicatesCount",
                                    modifier = Modifier.weight(1.3f),
                                    isHighlight = true
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = tr("Duplicate Clusters", "检索出的相似图片组", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (similarGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = tr("Perfect. No duplicates found in this album!", "太棒了，当前相册中未发现多余的相似文件！", lang),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(similarGroups) { group ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr("Similar Group ${group.id}", "相似分组 ${group.id}", lang),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    // Batch action button
                                    TextButton(
                                        onClick = { viewModel.deleteDuplicatesInGroup(context, group.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tr("Keep 1st, Delete Others", "保留第一张，删除其余", lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // List duplicate thumbnails side by side in a scrollable or even Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    group.photos.forEachIndexed { index, photo ->
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NordicCharcoal)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clickable { viewModel.showPreview(photo) }
                                            ) {
                                                PhotoThumbnail(photo = photo, modifier = Modifier.fillMaxSize())
                                                
                                                // Retain Badge on first photo
                                                if (index == 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(6.dp)
                                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            .align(Alignment.TopStart)
                                                    ) {
                                                        Text(
                                                            text = tr("Keep", "保留", lang),
                                                            fontSize = 10.sp,
                                                            color = SolarAmber,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            // Deletion Trigger
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.deletePhoto(context, photo.id) }
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = tr("Delete", "删除", lang),
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = tr("Delete", "删除", lang),
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

// TAB 3: Me & Settings Screen
@Composable
fun MeSettingsScreen(
    viewModel: HakimiViewModel,
    lang: AppLanguage
) {
    var showTerms by remember { mutableStateOf<String?>(null) } // "privacy" or "user" or null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "HAKIMI IMAGE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = tr("Me", "我的", lang),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // Custom minimal profile avatar
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column {
                    Text(
                        text = "Hakimi Image User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tr("Local Sandbox Active", "本地安全沙箱已激活", lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Settings items
        Text(
            text = tr("PREFERENCES", "通用首选项", lang),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Language Item
        SettingsItemCard(
            title = tr("Language", "系统显示语言", lang),
            description = tr("English (United States)", "简体中文 (中国)", lang),
            icon = Icons.Outlined.Translate,
            onClick = {
                val nextLang = if (lang == AppLanguage.ENGLISH) AppLanguage.SIMPLIFIED_CHINESE else AppLanguage.ENGLISH
                viewModel.setLanguage(nextLang)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = tr("ABOUT & TERMS", "关于与法律协议", lang),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Privacy Agreement
        SettingsItemCard(
            title = tr("Privacy Policy", "隐私政策协议", lang),
            description = tr("View local photo processing & data safety rules", "查看本地照片处理与数据安全说明", lang),
            icon = Icons.Outlined.Security,
            onClick = { showTerms = "privacy" }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // User Agreement
        SettingsItemCard(
            title = tr("User Agreement", "用户服务协议", lang),
            description = tr("View terms of service", "查看服务条款说明", lang),
            icon = Icons.Outlined.Assignment,
            onClick = { showTerms = "user" }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Rate
        SettingsItemCard(
            title = tr("Rate Us", "前往评价应用", lang),
            description = tr("Rate Hakimi Image on the Play Store", "前往应用商店评分", lang),
            icon = Icons.Outlined.StarOutline,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Share
        SettingsItemCard(
            title = tr("Share App", "分享当前应用", lang),
            description = tr("Share Hakimi Image with friends", "分享 Hakimi Image", lang),
            icon = Icons.Outlined.Share,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(48.dp))
    }

    // Modal Sheet for Terms & Privacy
    showTerms?.let { termType ->
        Dialog(onDismissRequest = { showTerms = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 500.dp),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (termType == "privacy") {
                            tr("Privacy Policy", "隐私政策与数据说明", lang)
                        } else {
                            tr("User Agreement", "服务条款与用户协议", lang)
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (termType == "privacy") {
                            tr(
                                "Hakimi Image strictly adheres to localized sandbox calculations. Your images, coordinates, face profiles, 128-D vectors, and Average Hash values NEVER leave your physical Android device. There are NO external server synchronizations or telemetry logs. 100% Offline.",
                                "Hakimi Image 严格执行本地沙盒计算。您的相册照片、人脸边界坐标、快照特征和平均哈希 (aHash) 指纹绝对不会上传或发送到任何外部服务器服务器。不收集任何敏感数据，100% 本地运行，为您守护纯粹的信息安全。",
                                lang
                            )
                        } else {
                            tr(
                                "By using Hakimi Image, you acknowledge that all sorting, similarity hashing, and portrait groupings are computed on-device in real-time. We are not liable for accidental hardware failures or file deletions authorized under system confirmation alerts.",
                                "当您使用 Hakimi Image 时，即代表您理解并同意应用内的所有分类统计、相似度对比、人物聚合均依靠您本地硬件的算力在本地执行。在执行相似删除或批量删除等操作时，应用将调用系统原生确认流程。对于经您授权后执行的物理删除，用户自担风险。",
                                lang
                            )
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showTerms = null },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = tr("Close", "我知道了", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

// METRIC WRAPPER
@Composable
fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// PREMIUM VECTOR DRAWING THUMBNAIL (Canvas-based)
@Composable
fun PhotoThumbnail(photo: PhotoItem, modifier: Modifier = Modifier) {
    if (photo.uri != null) {
        // Display real device photo with Coil
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.displayName,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Render custom Western high-fidelity visual vector on Canvas
        val cat = photo.category
        val primaryColor = MaterialTheme.colorScheme.primary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            // Render category backgrounds
            when (cat) {
                PhotoCategory.LANDSCAPE -> {
                    // Sunset Hills (Warm glow gradient)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF5E62), Color(0xFFFF9966))
                        )
                    )
                    // Draw a glowing sun
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = w * 0.22f,
                        center = Offset(w * 0.5f, h * 0.45f)
                    )
                    // Draw geometric dark valleys
                    val path1 = Path().apply {
                        moveTo(0f, h)
                        lineTo(w * 0.4f, h * 0.72f)
                        lineTo(w, h)
                        close()
                    }
                    val path2 = Path().apply {
                        moveTo(0f, h)
                        lineTo(w * 0.75f, h * 0.82f)
                        lineTo(w, h)
                        close()
                    }
                    drawPath(path1, Color(0xFF3A1C1C))
                    drawPath(path2, Color(0xFF201010))
                }

                PhotoCategory.PEOPLE -> {
                    // Portrait Face vector
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFDFD3), Color(0xFFFFA07A)),
                            center = Offset(w * 0.5f, h * 0.5f),
                            radius = w * 0.8f
                        )
                    )
                    // Draw geometric face background orb
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = w * 0.35f,
                        center = Offset(w * 0.5f, h * 0.45f)
                    )
                    // Simple, clean silhouette portrait
                    // Head circle
                    drawCircle(
                        color = Color(0xFF5D4037),
                        radius = w * 0.16f,
                        center = Offset(w * 0.5f, h * 0.38f)
                    )
                    // Shoulder curve
                    val shoulderPath = Path().apply {
                        moveTo(w * 0.15f, h)
                        quadraticTo(w * 0.5f, h * 0.65f, w * 0.85f, h)
                        close()
                    }
                    drawPath(shoulderPath, Color(0xFF5D4037))
                }

                PhotoCategory.ANIMAL -> {
                    // Pastel Greenish Brown
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE2906D), Color(0xFFE7C3A5))
                        )
                    )
                    // Draw cute simplified ears and snout shapes
                    val earsPath = Path().apply {
                        moveTo(w * 0.25f, h * 0.5f)
                        lineTo(w * 0.35f, h * 0.25f)
                        lineTo(w * 0.45f, h * 0.5f)
                        moveTo(w * 0.55f, h * 0.5f)
                        lineTo(w * 0.65f, h * 0.25f)
                        lineTo(w * 0.75f, h * 0.5f)
                    }
                    drawPath(earsPath, Color(0xFF5C3D2E), style = Stroke(width = w * 0.05f, join = StrokeJoin.Round))
                    drawCircle(
                        color = Color(0xFF5C3D2E),
                        radius = w * 0.2f,
                        center = Offset(w * 0.5f, h * 0.65f)
                    )
                }

                PhotoCategory.FOOD -> {
                    // Pizza / Pancake Golden Yellow
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF3A152), Color(0xFFECA034))
                        )
                    )
                    // Draw stylized ceramic plate
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = w * 0.38f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    // Draw tasty pancake stack or pizza circles
                    drawCircle(
                        color = Color(0xFF8B5A2B),
                        radius = w * 0.26f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    drawCircle(
                        color = Color(0xFFE5C158),
                        radius = w * 0.24f,
                        center = Offset(w * 0.5f, h * 0.48f)
                    )
                    // Red topping circle
                    drawCircle(
                        color = Color(0xFFD32F2F),
                        radius = w * 0.05f,
                        center = Offset(w * 0.45f, h * 0.42f)
                    )
                }

                PhotoCategory.VEHICLE -> {
                    // Dark cyber neon blue
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF243B55), Color(0xFF141E30))
                        )
                    )
                    // Neon lines
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(0f, h * 0.8f),
                        end = Offset(w, h * 0.8f),
                        strokeWidth = w * 0.03f
                    )
                    // Vehicle minimal shape
                    val carPath = Path().apply {
                        moveTo(w * 0.15f, h * 0.75f)
                        lineTo(w * 0.25f, h * 0.55f)
                        lineTo(w * 0.75f, h * 0.55f)
                        lineTo(w * 0.85f, h * 0.75f)
                        close()
                    }
                    drawPath(carPath, Color(0xFF00E5FF).copy(alpha = 0.3f))
                    drawPath(carPath, Color(0xFF00E5FF), style = Stroke(width = w * 0.03f))
                }

                PhotoCategory.ARCHITECTURE -> {
                    // Violet Skyline
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4568DC), Color(0xFFB06AB3))
                        )
                    )
                    // Geometric towers silhouettes
                    drawRect(
                        color = Color(0xFF1F1C2C),
                        topLeft = Offset(w * 0.15f, h * 0.4f),
                        size = Size(w * 0.3f, h * 0.6f)
                    )
                    drawRect(
                        color = Color(0xFF110F18),
                        topLeft = Offset(w * 0.5f, h * 0.3f),
                        size = Size(w * 0.35f, h * 0.7f)
                    )
                    // Draw small glowing windows
                    drawCircle(color = Color(0xFFFFD54F), radius = w * 0.015f, center = Offset(w * 0.22f, h * 0.5f))
                    drawCircle(color = Color(0xFFFFD54F), radius = w * 0.015f, center = Offset(w * 0.6f, h * 0.45f))
                }

                PhotoCategory.DOCUMENT -> {
                    // Cream grey document layout
                    drawRect(Color(0xFFECEFF1))
                    // Text blocks representing receipts
                    val lineStroke = w * 0.04f
                    drawLine(Color(0xFFB0BEC5), Offset(w * 0.15f, h * 0.25f), Offset(w * 0.85f, h * 0.25f), lineStroke)
                    drawLine(Color(0xFFB0BEC5), Offset(w * 0.15f, h * 0.42f), Offset(w * 0.7f, h * 0.42f), lineStroke)
                    drawLine(Color(0xFFB0BEC5), Offset(w * 0.15f, h * 0.60f), Offset(w * 0.8f, h * 0.60f), lineStroke)
                    // Barcode block at bottom
                    drawRect(Color(0xFF37474F), Offset(w * 0.15f, h * 0.78f), Size(w * 0.4f, h * 0.08f))
                }

                PhotoCategory.SPORTS -> {
                    // Dynamic Stadium Green
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
                        )
                    )
                    // Tennis racket/court lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(w * 0.15f, 0f),
                        end = Offset(w * 0.15f, h),
                        strokeWidth = w * 0.02f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(w * 0.85f, 0f),
                        end = Offset(w * 0.85f, h),
                        strokeWidth = w * 0.02f
                    )
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        radius = w * 0.12f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }

                else -> {
                    // Smooth Aurora fluid field for others
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor, tertiaryColor, Color(0xFF141E30)),
                            center = Offset(w * 0.3f, h * 0.4f),
                            radius = w * 1.2f
                        )
                    )
                }
            }
        }
    }
}

// DYNAMIC VECTOR HANDWRITTEN FACE PORTRAITS FOR SNAPSHOTS
@Composable
fun FaceSnapshotVector(faceId: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background color based on faceId
        val bgColors = listOf(
            listOf(Color(0xFFFF8C8C), Color(0xFFFFDFD3)),
            listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
            listOf(Color(0xFFf9d423), Color(0xFFff4e50)),
            listOf(Color(0xFFb1f2ff), Color(0xFFffd5ff))
        )
        val selectedBg = bgColors[abs(faceId % bgColors.size)]

        drawRect(
            brush = Brush.verticalGradient(selectedBg)
        )

        // Draw elegant mathematical features based on ID to look like stylized human line art
        val skinOutline = Color(0xFF2C3035)
        val lineWeight = w * 0.045f

        // Glasses or eyes
        if (faceId % 2 == 0) {
            // Draw cute round glasses
            drawCircle(skinOutline, w * 0.14f, Offset(w * 0.34f, h * 0.42f), style = Stroke(width = lineWeight))
            drawCircle(skinOutline, w * 0.14f, Offset(w * 0.66f, h * 0.42f), style = Stroke(width = lineWeight))
            drawLine(skinOutline, Offset(w * 0.48f, h * 0.42f), Offset(w * 0.52f, h * 0.42f), lineWeight)
        } else {
            // Draw standard squinting arches
            val eyePath1 = Path().apply {
                moveTo(w * 0.22f, h * 0.44f)
                quadraticTo(w * 0.32f, h * 0.38f, w * 0.42f, h * 0.44f)
            }
            val eyePath2 = Path().apply {
                moveTo(w * 0.58f, h * 0.44f)
                quadraticTo(w * 0.68f, h * 0.38f, w * 0.78f, h * 0.44f)
            }
            drawPath(eyePath1, skinOutline, style = Stroke(width = lineWeight, cap = StrokeCap.Round))
            drawPath(eyePath2, skinOutline, style = Stroke(width = lineWeight, cap = StrokeCap.Round))
        }

        // Smiling nose & mouth
        val smilePath = Path().apply {
            moveTo(w * 0.35f, h * 0.68f)
            quadraticTo(w * 0.5f, h * 0.82f, w * 0.65f, h * 0.68f)
        }
        drawPath(smilePath, skinOutline, style = Stroke(width = lineWeight, cap = StrokeCap.Round))

        // Cute blush dots
        drawCircle(Color(0xFFFF5E62).copy(alpha = 0.5f), w * 0.08f, Offset(w * 0.22f, h * 0.58f))
        drawCircle(Color(0xFFFF5E62).copy(alpha = 0.5f), w * 0.08f, Offset(w * 0.78f, h * 0.58f))
    }
}

private fun abs(n: Int): Int {
    return if (n < 0) -n else n
}

// IMAGE PREVIEW MODAL DIALOG OVERLAY WITH ZOOM/METRICS
@Composable
fun ImagePreviewDialog(
    photo: PhotoItem,
    onDismiss: () -> Unit,
    lang: AppLanguage
) {
    val dateStr = remember(photo.dateAdded) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(photo.dateAdded * 1000))
    }

    val sizeStr = remember(photo.size) {
        val kb = photo.size / 1024
        if (kb > 1024) {
            String.format("%.2f MB", kb.toFloat() / 1024)
        } else {
            "$kb KB"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Image container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NordicCharcoal)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                ) {
                    PhotoThumbnail(photo = photo, modifier = Modifier.fillMaxSize())
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Metadata list
                Text(
                    text = photo.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                PreviewMetaRow(label = tr("Category", "归属分类", lang), value = photo.category.getDisplayName(lang == AppLanguage.ENGLISH))
                PreviewMetaRow(label = tr("File Size", "文件大小", lang), value = sizeStr)
                PreviewMetaRow(label = tr("Created Date", "创建时间", lang), value = dateStr)
                PreviewMetaRow(
                    label = tr("Security Hash", "安全平均哈希指纹", lang), 
                    value = photo.averageHash,
                    isMonospace = true
                )

                if (photo.faces.isNotEmpty()) {
                    PreviewMetaRow(
                        label = tr("Detected Faces", "检测到脸部数量", lang), 
                        value = "${photo.faces.size} faces (Local Sandboxed)"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = tr("Close", "我知道了", lang), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun PreviewMetaRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Icon Mapping helper
fun getCategoryIcon(cat: PhotoCategory): ImageVector {
    return when (cat) {
        PhotoCategory.PEOPLE -> Icons.Outlined.Face
        PhotoCategory.LANDSCAPE -> Icons.Outlined.Landscape
        PhotoCategory.ANIMAL -> Icons.Outlined.Pets
        PhotoCategory.FOOD -> Icons.Outlined.Restaurant
        PhotoCategory.VEHICLE -> Icons.Outlined.DirectionsCar
        PhotoCategory.ARCHITECTURE -> Icons.Outlined.Apartment
        PhotoCategory.DOCUMENT -> Icons.Outlined.Description
        PhotoCategory.SPORTS -> Icons.Outlined.SportsSoccer
        PhotoCategory.OTHER -> Icons.Outlined.Collections
    }
}
