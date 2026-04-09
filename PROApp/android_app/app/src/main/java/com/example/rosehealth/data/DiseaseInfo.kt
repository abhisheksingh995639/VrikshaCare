package com.example.rosehealth.data

import androidx.compose.ui.graphics.Color

data class DiseaseInfo(
    val name: String,
    val color: Color,
    val icon: String,
    val cause: String,
    val treatment: String,
    val severity: String
)

object DiseaseData {
    val infoMap = mapOf(
        "Black_Spot" to DiseaseInfo(
            name = "Black Spot",
            color = Color(0xFFE74C3C),
            icon = "🔴",
            cause = "Fungal infection (Diplocarpon rosae)",
            treatment = "Remove infected leaves, apply fungicide (copper-based or neem oil). Improve air circulation.",
            severity = "High"
        ),
        "Downy_Mildew" to DiseaseInfo(
            name = "Downy Mildew",
            color = Color(0xFF3498DB),
            icon = "🔵",
            cause = "Water mold (Peronospora sparsa) spreading in humid conditions",
            treatment = "Improve airflow, avoid overhead watering, remove infected parts, apply systemic fungicides.",
            severity = "High"
        ),
        "Dry_Leaf" to DiseaseInfo(
            name = "Dry Leaf",
            color = Color(0xFFE67E22),
            icon = "🟠",
            cause = "Underwatering, heat stress or root issues",
            treatment = "Water deeply and regularly. Check soil drainage. Add mulch to retain moisture.",
            severity = "Medium"
        ),
        "Healthy_Leaf" to DiseaseInfo(
            name = "Healthy Leaf",
            color = Color(0xFF27AE60),
            icon = "🟢",
            cause = "No disease detected",
            treatment = "Continue regular care: water, fertilize, prune as needed.",
            severity = "None"
        ),
        "Leaf_Holes" to DiseaseInfo(
            name = "Leaf Holes",
            color = Color(0xFF8E44AD),
            icon = "🟣",
            cause = "Insect damage (caterpillars, beetles) or shot-hole fungus",
            treatment = "Inspect for insects and remove manually. Apply neem oil or insecticidal soap spray.",
            severity = "Medium"
        )
    )
    
    fun getInfo(name: String): DiseaseInfo {
        return infoMap[name] ?: DiseaseInfo(
            name = "Unknown",
            color = Color.Gray,
            icon = "❓",
            cause = "Unknown",
            treatment = "N/A",
            severity = "N/A"
        )
    }
}
