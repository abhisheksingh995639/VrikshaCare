package com.example.rosehealth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rosehealth.data.DiseaseData
import com.example.rosehealth.ui.VrikshaCareTypography
import java.io.File

// ── Route constants ───────────────────────────────────────────────────────────

object Routes {
    const val SCAN      = "scan"
    const val DIAGNOSIS = "diagnosis"
    const val SETTINGS  = "settings"
}

// ── Design tokens — updated to match Stitch "Living Monograph" palette ────────

/** Deep Forest Green — buttons, icons, primary text */
val ForestGreen     = Color(0xFF2D5A27)
/** Very dark green — authoritative headings & text */
val ForestDark      = Color(0xFF154212)
/** Earthy gold — badges, accents, section labels */
val SunlightGold    = Color(0xFFC8A951)
/** Botanical cream — page background */
val ForestSurface   = Color(0xFFF8FAF3)
/** Soft green-grey — card / container fills */
val ForestContainer = Color(0xFFEDEFE8)
/** Pure white — elevated card surfaces */
val CardWhite       = Color(0xFFFFFFFF)

@Composable
fun ForestEtherTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary             = ForestGreen,
        onPrimary           = Color.White,
        primaryContainer    = ForestDark,
        onPrimaryContainer  = Color.White,
        secondary           = SunlightGold,
        onSecondary         = Color.White,
        surface             = ForestSurface,
        onSurface           = ForestDark,
        surfaceVariant      = ForestContainer,
        onSurfaceVariant    = ForestDark.copy(alpha = 0.75f)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = VrikshaCareTypography,   // ← Noto Serif headings applied
        content     = content
    )
}

// ── Localizations ─────────────────────────────────────────────────────────────

data class AppStrings(
    val scan: String,
    val analyze: String,
    val gallery: String,
    val camera: String,
    val diagnosis: String,
    val settings: String,
    val appLanguage: String,
    val modelInfo: String,
    val syncStatus: String,
    val engineStatus: String,
    val noImage: String,
    val analyzeTip: String,
    val diseases: Map<String, String>,
    // Diagnosis screen labels
    val detectedDisease: String,
    val confidence: String,
    val confidenceScores: String,
    val severity: String,
    val cause: String,
    val treatment: String,
    val scanAnother: String,
    val viewFullDiagnosis: String
)

val Localizations = mapOf(
    "English" to AppStrings(
        scan = "VrikshaCare", analyze = "Analyze", gallery = "Gallery", camera = "Camera",
        diagnosis = "DIAGNOSIS", settings = "Settings", appLanguage = "APP LANGUAGE",
        modelInfo = "VrikshaModel v3", syncStatus = "V3 Index Synced", engineStatus = "AI Engine Online",
        noImage = "No Image Selected", analyzeTip = "Tap Analyze to detect disease.",
        diseases = mapOf(
            "blackspot" to "Black Spot", "downymildew" to "Downy Mildew", "leafholes" to "Leaf Holes",
            "healthyleaf" to "Healthy Leaf", "dryleaf" to "Dry Leaf", "redrust" to "Red Rust", "freshleaf" to "Healthy Leaf"
        ),
        detectedDisease = "DETECTED DISEASE",
        confidence = "Confidence",
        confidenceScores = "CONFIDENCE SCORES",
        severity = "Severity",
        cause = "Cause",
        treatment = "Treatment",
        scanAnother = "📷  Scan Another Leaf",
        viewFullDiagnosis = "🔬  View Full Diagnosis"
    ),
    "Hindi" to AppStrings(
        scan = "वृक्षाकेयर", analyze = "विश्लेषण करें", gallery = "गैलरी", camera = "कैमरा",
        diagnosis = "निदान", settings = "सेटिंग्स", appLanguage = "अनुप्रयोग भाषा",
        modelInfo = "वृक्षा मॉडल v3", syncStatus = "V3 अनुक्रमणिका Synced", engineStatus = "एआई इंजन सक्रिय",
        noImage = "कोई चित्र नहीं चुना", analyzeTip = "रोग पहचानने के लिए विश्लेषण करें।",
        diseases = mapOf(
            "blackspot" to "काला धब्बा", "downymildew" to "डाउनी मिल्ड्यू", "leafholes" to "पत्तों में छेद",
            "healthyleaf" to "स्वस्थ पत्ता", "dryleaf" to "सूखा पत्ता", "redrust" to "लाल जंग", "freshleaf" to "स्वस्थ पत्ता"
        ),
        detectedDisease = "पहचाना गया रोग",
        confidence = "विश्वास",
        confidenceScores = "विश्वास स्कोर",
        severity = "गंभीरता",
        cause = "कारण",
        treatment = "उपचार",
        scanAnother = "📷  दूसरा पत्ता स्कैन करें",
        viewFullDiagnosis = "🔬  पूर्ण निदान देखें"
    )
)

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private var classifier: Classifier? = null
    private var initError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            classifier = Classifier(this, "rose_model_v3.tflite", "labels.txt")
        } catch (e: Exception) {
            initError = e.message ?: "Failed to initialize TFLite"
        }
        setContent { RoseHealthApp(classifier, initError) }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier?.close()
    }
}

// ── Root composable with NavHost ──────────────────────────────────────────────

@Composable
fun RoseHealthApp(classifier: Classifier?, initialError: String?) {
    val navController = rememberNavController()
    val vm: AppViewModel = viewModel()
    val context = LocalContext.current

    val bitmap         by vm.bitmap.collectAsStateWithLifecycle()
    val results        by vm.results.collectAsStateWithLifecycle()
    val selectedLang   by vm.selectedLanguage.collectAsStateWithLifecycle()
    val strings = Localizations[selectedLang] ?: Localizations["English"]!!

    var isLoading    by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf(initialError) }

    // Camera file / URI setup (must live in composable scope)
    val cameraImageFile = remember {
        File(context.cacheDir, "images").also { it.mkdirs() }.let { File(it, "camera_photo.jpg") }
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", cameraImageFile)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.setBitmap(decodeBitmap(context, it)) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            try { vm.setBitmap(decodeBitmap(context, cameraUri)) }
            catch (e: Exception) { currentError = "Error loading photo: ${e.localizedMessage}" }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try { cameraLauncher.launch(cameraUri) }
            catch (e: Exception) { currentError = "Camera fail: ${e.localizedMessage}" }
        } else {
            currentError = "Camera permission denied"
        }
    }

    ForestEtherTheme {
        NavHost(
            navController = navController,
            startDestination = Routes.SCAN
        ) {
            // ── Scan screen ──────────────────────────────────────────────
            composable(Routes.SCAN) {
                ScanScreen(
                    bitmap        = bitmap,
                    results       = results,
                    isLoading     = isLoading,
                    currentError  = currentError,
                    strings       = strings,
                    onGalleryClick = { galleryLauncher.launch("image/*") },
                    onCameraClick  = {
                        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            try { cameraLauncher.launch(cameraUri) }
                            catch (e: Exception) { currentError = "Intent failed: ${e.localizedMessage}" }
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onAnalyzeClick = {
                        if (bitmap != null && classifier != null) {
                            isLoading = true
                            vm.setResults(classifier.classify(bitmap!!))
                            isLoading = false
                        }
                    },
                    onViewFullDiagnosis = { navController.navigate(Routes.DIAGNOSIS) },
                    onSettingsClick     = { navController.navigate(Routes.SETTINGS) }
                )
            }

            // ── Diagnosis detail screen ──────────────────────────────────
            composable(Routes.DIAGNOSIS) {
                DiagnosisScreen(
                    bitmap      = bitmap,
                    results     = results,
                    strings     = strings,
                    onBack      = { navController.popBackStack() },
                    onScanAnother = {
                        vm.setBitmap(null)
                        navController.popBackStack()
                    }
                )
            }

            // ── Settings screen ──────────────────────────────────────────
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    selectedLanguage = selectedLang,
                    strings          = strings,
                    onLanguageSelect = { vm.setLanguage(it) },
                    onBack           = { navController.popBackStack() }
                )
            }
        }
    }
}

// ── ScanScreen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    bitmap: Bitmap?,
    results: List<Pair<String, Float>>,
    isLoading: Boolean,
    currentError: String?,
    strings: AppStrings,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onViewFullDiagnosis: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("🌿 ${strings.scan}", fontWeight = FontWeight.Bold, color = ForestGreen)
                },
                navigationIcon = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Settings", tint = ForestGreen)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ForestSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ForestSurface)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ForestContainer),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected Leaf",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = ForestGreen.copy(alpha = 0.3f))
                        Text(strings.noImage, color = ForestGreen.copy(alpha = 0.4f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results summary card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = ForestContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (results.isNotEmpty()) strings.diagnosis else strings.analyzeTip,
                        style = MaterialTheme.typography.labelMedium,
                        color = SunlightGold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (results.isNotEmpty()) {
                        val rawLabel    = results[0].first.lowercase().substringAfter("v3_").trim()
                        val slugKey     = rawLabel.replace(" ", "").replace("-", "")
                        val diseaseKey  = rawLabel.split("_", " ").joinToString("_") { it.replaceFirstChar { c -> c.uppercase() } }
                        val diseaseInfo = DiseaseData.getInfo(diseaseKey)
                        val translatedLabel = strings.diseases[slugKey] ?: rawLabel.replace("_", " ").uppercase()
                        val topConf     = results[0].second

                        Text(
                            text = "${diseaseInfo.icon} $translatedLabel",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = diseaseInfo.color
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick bars
                        results.take(3).forEach { result ->
                            val itemRaw      = result.first.lowercase().substringAfter("v3_").trim()
                            val itemSlugKey  = itemRaw.replace(" ", "").replace("-", "")
                            val itemLabel    = strings.diseases[itemSlugKey] ?: itemRaw.replace("_", " ")
                            ResultRow(itemLabel, result.second, SunlightGold)
                        }
                        if (results.size > 3) {
                            Text(
                                "+ ${results.size - 3} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick summary
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ForestGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DiseaseInfoRow("Confidence", "${"%.1f".format(topConf * 100)}%")
                                DiseaseInfoRow("Severity", diseaseInfo.severity, valueColor = when (diseaseInfo.severity) {
                                    "High"   -> Color(0xFFE74C3C)
                                    "Medium" -> Color(0xFFE67E22)
                                    "None"   -> Color(0xFF27AE60)
                                    else     -> ForestGreen
                                })
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── View Full Diagnosis button ──────────────────────
                        Button(
                            onClick      = onViewFullDiagnosis,
                            modifier     = Modifier.fillMaxWidth().height(44.dp),
                            colors       = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape        = RoundedCornerShape(50)
                        ) {
                            Text(strings.viewFullDiagnosis, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(strings.analyzeTip, color = ForestGreen.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Analyze button
            Button(
                onClick  = onAnalyzeClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled  = bitmap != null && !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = SunlightGold),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(strings.analyze, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick  = onGalleryClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape    = RoundedCornerShape(25.dp)
                ) {
                    Text(strings.gallery, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Button(
                    onClick  = onCameraClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape    = RoundedCornerShape(25.dp)
                ) {
                    Text(strings.camera, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            currentError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ── SettingsScreen — full Stitch design applied ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedLanguage: String,
    strings: AppStrings,
    onLanguageSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    val languages = listOf("English", "Hindi")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        strings.settings,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ForestDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ForestSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ForestSurface)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── App info gradient card ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ForestDark, ForestGreen),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌿", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "VrikshaCare",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "AI Plant Health Companion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "v3.0  •  VrikshaModel v3",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // ── Language section ──────────────────────────────────────────
            SettingsSectionLabel(strings.appLanguage)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(languages) { lang ->
                    LanguageChip(
                        name       = lang,
                        isSelected = selectedLanguage == lang,
                        onClick    = { onLanguageSelect(lang) }
                    )
                }
            }

            // ── AI Preferences section ────────────────────────────────────
            SettingsSectionLabel("AI PREFERENCES")
            Surface(
                modifier      = Modifier.fillMaxWidth(),
                color         = CardWhite,
                shape         = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsCardRow(icon = "🤖", title = "Inference Model", subtitle = strings.modelInfo)
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = ForestContainer)
                    SettingsCardRow(icon = "🏷️", title = "Label Set", subtitle = strings.syncStatus, dotColor = ForestGreen)
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = ForestContainer)
                    SettingsCardRow(icon = "⚙️", title = "Engine Status", subtitle = strings.engineStatus, subtitleColor = Color(0xFF2E7D32), dotColor = Color(0xFF4CAF50))
                }
            }

            // ── About section ─────────────────────────────────────────────
            SettingsSectionLabel("ABOUT")
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                color           = CardWhite,
                shape           = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsNavRow(icon = "📋", title = "Privacy Policy")
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = ForestContainer)
                    SettingsNavRow(icon = "⭐", title = "Rate the App")
                }
            }

            // ── Footer ────────────────────────────────────────────────────
            Text(
                "Made with 🌱 for plant enthusiasts",
                modifier  = Modifier.fillMaxWidth(),
                style     = MaterialTheme.typography.bodySmall,
                color     = ForestGreen.copy(alpha = 0.45f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Settings sub-composables ──────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelSmall,
        color         = SunlightGold,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun SettingsCardRow(
    icon: String,
    title: String,
    subtitle: String,
    subtitleColor: Color = ForestDark.copy(alpha = 0.6f),
    dotColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ForestDark)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
        }
    }
}

@Composable
private fun SettingsNavRow(icon: String, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style    = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color    = ForestDark,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint   = ForestGreen.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) SunlightGold else ForestContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = name,
                color = if (isSelected) Color.White else ForestGreen,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(title: String, subtitle: String, color: Color = ForestGreen) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        color = ForestContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ForestGreen)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun ResultRow(label: String, confidence: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = ForestGreen.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
        Text("${(confidence * 100).toInt()}%", color = ForestGreen, modifier = Modifier.padding(end = 8.dp))
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape).background(ForestContainer)
        ) {
            CircularProgressIndicator(
                progress = confidence,
                modifier = Modifier.fillMaxSize(),
                color = if (confidence > 0.7) SunlightGold else Color.Gray,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun DiseaseInfoRow(label: String, value: String, valueColor: Color = ForestGreen) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = SunlightGold,
            modifier = Modifier.widthIn(min = 80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Utility ───────────────────────────────────────────────────────────────────

private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT < 28) {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } else {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    }.copy(Bitmap.Config.ARGB_8888, true)
}
