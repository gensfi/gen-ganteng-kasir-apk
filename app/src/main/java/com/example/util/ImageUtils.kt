package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

data class PresetPhoto(
    val name: String,
    val url: String
)

object ImageUtils {

    val PRESET_PHOTOS = listOf(
        PresetPhoto(
            name = "Espresso",
            url = "https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=600&q=80"
        ),
        PresetPhoto(
            name = "Iced Americano",
            url = "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=600&q=80"
        ),
        PresetPhoto(
            name = "Kopi Susu Aren",
            url = "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=600&q=80"
        ),
        PresetPhoto(
            name = "Latte / Cappuccino",
            url = "https://images.unsplash.com/photo-1534778101976-62847782c213?w=600&q=80"
        ),
        PresetPhoto(
            name = "Caramel Macchiato",
            url = "https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=600&q=80"
        ),
        PresetPhoto(
            name = "Matcha Latte",
            url = "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&q=80"
        ),
        PresetPhoto(
            name = "Iced Tea / Lemon Tea",
            url = "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&q=80"
        ),
        PresetPhoto(
            name = "Butter Croissant",
            url = "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&q=80"
        ),
        PresetPhoto(
            name = "Toast / Roti Panggang",
            url = "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=600&q=80"
        ),
        PresetPhoto(
            name = "Waffle & Pancake",
            url = "https://images.unsplash.com/photo-1562376552-0d160a2f238d?w=600&q=80"
        ),
        PresetPhoto(
            name = "French Fries / Kentang",
            url = "https://images.unsplash.com/photo-1576107232684-1279f3908594?w=600&q=80"
        ),
        PresetPhoto(
            name = "Burger / Sandwich",
            url = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&q=80"
        )
    )

    fun saveImageFromUri(context: Context, sourceUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "menu_images").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(imagesDir, "menu_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            "file://${destinationFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
