package com.example.readers_diary


import android.os.Bundle
import com.example.readerdiary.Book
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import javax.inject.Inject

class BookDetailsActivity : AppCompatActivity() {

    private lateinit var currentBook: Book
    private lateinit var bookRepository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.read)

        // Инициализация репозитория
        bookRepository = BookRepositoryImpl(applicationContext)

        // Получаем книгу из Intent (предполагается, что передается ID книги)
        val bookId = intent.getStringExtra("BOOK_ID") ?: return finish()
        currentBook = bookRepository.loadBooks().firstOrNull { it.id == bookId } ?: return finish()

        // Инициализация UI
        setupViews()
        setupListeners()
        updateUI()
    }

    private fun setupViews() {
        // Заполняем данные книги
        tvBookTitle.text = currentBook.title
        tvBookAuthor.text = "Автор: ${currentBook.author}"
        tvTotalPages.text = "Всего страниц: ${currentBook.totalPages}"
        updatePagesInfo()

        // Настройка RatingBar
        ratingBar.rating = currentBook.rating
        tvRatingValue.text = "%.1f".format(currentBook.rating)

        // Настройка статуса
        setupStatusDropdown()

        // Настройка прогресс-бара
        progressBarReading.max = currentBook.totalPages
        progressBarReading.progress = currentBook.readPages

        // Настройка краткого содержания
        etBookSummary.setText(currentBook.summary)
    }

    private fun setupStatusDropdown() {
        val statuses = BookStatus.values().map { it.toString() }
        val adapter = ArrayAdapter(this, R.layout.dropdown_menu_item, statuses)
        statusDropdown.setAdapter(adapter)
        statusDropdown.setText(currentBook.status.toString(), false)
    }

    private fun setupListeners() {
        // Сохранение прогресса чтения
        btnSaveProgress.setOnClickListener {
            saveReadingProgress()
        }

        // Сохранение рейтинга
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            currentBook.rating = rating
            tvRatingValue.text = "%.1f".format(rating)
            saveBook()
        }

        // Сохранение статуса
        statusDropdown.setOnItemClickListener { _, _, position, _ ->
            currentBook.status = BookStatus.values()[position]
            saveBook()
        }

        // Добавление/редактирование краткого содержания
        btnAddSummary.setOnClickListener {
            summaryContainer.visibility = if (summaryContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Сохранение краткого содержания при потере фокуса
        etBookSummary.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                currentBook.summary = etBookSummary.text.toString()
                saveBook()
            }
        }

        // Кнопка назад
        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun saveReadingProgress() {
        val pagesReadText = etPagesRead.text.toString()
        if (pagesReadText.isBlank()) {
            Toast.makeText(this, "Введите количество страниц", Toast.LENGTH_SHORT).show()
            return
        }

        val pagesRead = pagesReadText.toInt()
        if (pagesRead < 0 || pagesRead > currentBook.totalPages) {
            Toast.makeText(this, "Некорректное количество страниц", Toast.LENGTH_SHORT).show()
            return
        }

        currentBook.readPages = pagesRead
        saveBook()
        updatePagesInfo()

        // Анимация успешного сохранения
        btnSaveProgress.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                btnSaveProgress.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun updatePagesInfo() {
        tvPagesLeft.text = "Осталось: ${currentBook.totalPages - currentBook.readPages} страниц"
        progressBarReading.progress = currentBook.readPages

        // Обновление статуса автоматически
        when {
            currentBook.readPages == 0 -> currentBook.status = BookStatus.PLANNED
            currentBook.readPages == currentBook.totalPages -> currentBook.status = BookStatus.FINISHED
            else -> currentBook.status = BookStatus.READING
        }
        statusDropdown.setText(currentBook.status.toString(), false)
    }

    private fun saveBook() {
        val books = bookRepository.loadBooks().toMutableList()
        val index = books.indexOfFirst { it.id == currentBook.id }
        if (index != -1) {
            books[index] = currentBook
            bookRepository.saveBooks(books)
        }
    }

    private fun updateUI() {
        // Обновляем поле ввода прочитанных страниц текущим значением
        etPagesRead.setText(currentBook.readPages.toString())
    }
}