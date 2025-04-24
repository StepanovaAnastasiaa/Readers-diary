package com.example.readers_diary

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.readerdiary.Book
import com.example.readerdiary.BookStatus

class AddBookActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextAuthor: EditText
    private lateinit var editTextPages: EditText
    private lateinit var buttonAddBook: Button
    private lateinit var btnBack: ImageButton

    private lateinit var bookRepository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_book)

        bookRepository = BookRepositoryImpl(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextAuthor = findViewById(R.id.editTextAuthor)
        editTextPages = findViewById(R.id.editTextPages)
        buttonAddBook = findViewById(R.id.buttonAddBook)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        buttonAddBook.setOnClickListener {
            createNewBook()
        }
    }

    private fun createNewBook() {
        // Получаем данные из полей ввода
        val title = editTextTitle.text.toString().trim()
        val author = editTextAuthor.text.toString().trim()
        val pagesText = editTextPages.text.toString().trim()

        // Валидация данных
        if (title.isEmpty()) {
            showError("Введите название книги")
            return
        }

        if (author.isEmpty()) {
            showError("Введите автора книги")
            return
        }

        if (pagesText.isEmpty()) {
            showError("Введите количество страниц")
            return
        }

        val pages = pagesText.toIntOrNull()
        if (pages == null || pages <= 0) {
            showError("Введите корректное количество страниц")
            return
        }

        // Создаем новую книгу
        val newBook = Book(
            title = title,
            author = author,
            totalPages = pages,
            readPages = 0,
            status = BookStatus.PLANNED,
            rating = 0f,
            summary = ""
        )

        // Сохраняем книгу
        saveBook(newBook)

        // Возвращаемся на предыдущий экран
        setResult(RESULT_OK)
        finish()
    }

    private fun saveBook(book: Book) {
        val currentBooks = bookRepository.loadBooks().toMutableList()
        currentBooks.add(book)
        bookRepository.saveBooks(currentBooks)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}