package com.example.rosehealth

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rosehealth.data.DiseaseData
import com.example.rosehealth.data.DiseaseInfo

// ── Severity helpers ─────────────────────────────────────────────────────────

private fun severityColor(severity: String) = when (severity) {
    "High"   -> Color(0xFFB71C1C)
    "Medium" -> Color(0xFFE65100)
    "None"   -> Color(0xFF2E7D32)
    else     -> Color(0xFF37474F)
}

private fun severityBg(severity: String) = when (severity) {
    "High"   -> Color(0xFFFFEBEE)
    "Medium" -> Color(0xFFFFF3E0)
    "None"   -> Color(0xFFE8F5E9)
    else     -> Color(0xFFECEFF1)
}

// ── DiagnosisScreen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisScreen(
    bitmap: Bitmap?,
    results: List<Pair<String, Float>>,
    strings: AppStrings,
    onBack: () -> Unit,
    onScanAnother: () -> Unit
) {
    // Derive disease info from top result
    val topResult = results.firstOrNull()
    val rawLabel   = topResult?.first?.lowercase()?.substringAfter("v3_")?.trim() ?: ""
    val slugKey    = rawLabel.replace(" ", "").replace("-", "")
    val diseaseKey = rawLabel.split("_", " ").joinToString("_") { it.replaceFirstChar { c -> c.uppercase() } }
    val diseaseInfo: DiseaseInfo = DiseaseData.getInfo(diseaseKey)
    val translatedLabel = strings.diseases[slugKey] ?: rawLabel.replace("_", " ").uppercase()
    val topConf    = topResult?.second ?: 0f

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "🌿 VrikshaCare",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ForestGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ForestGreen)
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
        ) {
            // ── Hero leaf image card ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ForestContainer)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Scanned Leaf",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // gradient scrim bottom for text legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                    startY = 300f
                                )
                            )
                    )
                }
                // Severity badge — bottom-left corner overlay
                SeverityBadge(
                    severity = diseaseInfo.severity,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            // ── Disease header ──────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = strings.detectedDisease,
                    style = MaterialTheme.typography.labelSmall,
                    color = SunlightGold,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${diseaseInfo.icon} $translatedLabel",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = diseaseInfo.color,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Confidence pill row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strings.confidence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForestGreen.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SunlightGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${"%.1f".format(topConf * 100)}%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = SunlightGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Diagnosis info card ─────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DiagnosisInfoRow(
                        icon = "🌿",
                        label = strings.severity,
                        content = {
                            SeverityBadge(severity = diseaseInfo.severity)
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFEEEEEE))
                    DiagnosisInfoRow(
                        icon = "🔬",
                        label = strings.cause,
                        content = {
                            Text(
                                text = diseaseInfo.cause,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF555555)
                            )
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFEEEEEE))
                    DiagnosisInfoRow(
                        icon = "💊",
                        label = strings.treatment,
                        content = {
                            Text(
                                text = diseaseInfo.treatment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF555555)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Confidence scores section ───────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = strings.confidenceScores,
                    style = MaterialTheme.typography.labelSmall,
                    color = SunlightGold,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 3.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        results.forEach { (label, conf) ->
                            val itemRaw       = label.lowercase().substringAfter("v3_").trim()
                            val itemSlugKey   = itemRaw.replace(" ", "").replace("-", "")
                            val itemLabel     = strings.diseases[itemSlugKey] ?: itemRaw.replace("_", " ").replaceFirstChar { it.uppercase() }
                            val itemDiseaseKey = itemRaw.split("_", " ").joinToString("_") { it.replaceFirstChar { c -> c.uppercase() } }
                            val itemInfo      = DiseaseData.getInfo(itemDiseaseKey)
                            val isTop         = conf == topConf

                            AnimatedConfidenceRow(
                                icon       = itemInfo.icon,
                                label      = itemLabel,
                                confidence = conf,
                                barColor   = if (isTop) ForestGreen else Color(0xFFBDBDBD),
                                isTop      = isTop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Scan Another Leaf button ────────────────────────────────────
            OutlinedButton(
                onClick = onScanAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 2.dp
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
            ) {
                Text(
                    strings.scanAnother,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "Made with 🌱 for plant enthusiasts",
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = ForestGreen.copy(alpha = 0.45f),
                fontStyle = FontStyle.Italic,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SeverityBadge(severity: String, modifier: Modifier = Modifier) {
    val icon = when (severity) {
        "High"   -> "⚠️"
        "Medium" -> "⚡"
        "None"   -> "✅"
        else     -> "❓"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = severityBg(severity)
    ) {
        Text(
            text = "$icon $severity",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = severityColor(severity)
        )
    }
}

@Composable
private fun DiagnosisInfoRow(
    icon: String,
    label: String,
    content: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = icon, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ForestGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun AnimatedConfidenceRow(
    icon: String,
    label: String,
    confidence: Float,
    barColor: Color,
    isTop: Boolean
) {
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animate = true }
    val animated by animateFloatAsState(
        targetValue = if (animate) confidence else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "bar_$label"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isTop) ForestGreen else Color(0xFF888888),
                fontWeight = if (isTop) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${"%.1f".format(confidence * 100)}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (isTop) SunlightGold else Color(0xFFAAAAAA),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        // Vine-stem style progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFF0F0F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isTop)
                            Brush.horizontalGradient(listOf(ForestGreen.copy(alpha = 0.6f), ForestGreen))
                        else
                            Brush.horizontalGradient(listOf(barColor, barColor))
                    )
            )
        }
    }
}
