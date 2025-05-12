// app/src/main/java/com/example/readerdiary/Book.kt
package com.example.readerdiary

import android.net.Uri

data class Book(
    // Основная информация
    val id: String = generateId(),
    var title: String = "",
    var author: String = "",
    var totalPages: Int = 0,
    var readPages: Int = 0,

    // Обложка
    var coverImagePath: String? = null, // путь к локальному файлу
    var coverImageUri: String? = null,     // URI изображения

    // Статус и прогресс
    var status: BookStatus = BookStatus.PLANNED,
    var lastUpdated: Long = System.currentTimeMillis(),

    // Описание
    var summary: String = "",

    // Дополнительные метаданные
    var addedDate: Long = System.currentTimeMillis(),
    var rating: Float = 0f
) {
    val progress: Int
        get() = if (totalPages > 0) {
            (readPages.toFloat() / totalPages * 100).toInt()
        } else {
            0
        }

    val pagesLeft: Int
        get() = totalPages - readPages

    companion object {
        private fun generateId(): String {
            return "book_${System.currentTimeMillis()}"
        }
    }

    fun getProgressPercentage(): Int {
        return if (totalPages > 0) {
            (readPages.toFloat() / totalPages * 100).toInt()
        } else {
            0
        }
    }
}

enum class BookStatus {
    PLANNED,      // В планах
    READING,      // Читаю
    FINISHED,     // Прочитано
}