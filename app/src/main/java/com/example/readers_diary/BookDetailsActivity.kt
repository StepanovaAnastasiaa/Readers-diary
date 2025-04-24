package com.example.readers_diary

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.readerdiary.Book
import com.example.readerdiary.BookStatus
import com.example.readers_diary.databinding.ReadBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BookDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ReadBinding
    private lateinit var currentBook: Book
    private lateinit var bookRepository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookRepository = BookRepositoryImpl(applicationContext)

        val bookId = intent.getStringExtra("BOOK_ID") ?: run {
            finish()
            return
        }

        currentBook = bookRepository.loadBooks().firstOrNull { it.id == bookId } ?: run {
            finish()
            return
        }

        setupViews()
        setupListeners()
        updateUI()
    }

    private fun setupViews() {
        binding.tvBookTitle.text = currentBook.title
        binding.tvBookAuthor.text = "Автор: ${currentBook.author}"
        binding.tvTotalPages.text = "Всего страниц: ${currentBook.totalPages}"
        updatePagesInfo()

        binding.ratingBar.rating = currentBook.rating
        binding.tvRatingValue.text = "%.1f".format(currentBook.rating)

        setupStatusDropdown()

        binding.progressBarReading.max = currentBook.totalPages
        binding.progressBarReading.progress = currentBook.readPages

        binding.etBookSummary.setText(currentBook.summary)
    }

    private fun setupStatusDropdown() {
        val statuses = BookStatus.values().map { it.toString() }
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, statuses)
        binding.statusDropdown.setAdapter(adapter)
        binding.statusDropdown.setText(currentBook.status.toString(), false)
    }

    private fun setupListeners() {
        binding.btnSaveProgress.setOnClickListener {
            saveReadingProgress()
        }

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            currentBook.rating = rating
            binding.tvRatingValue.text = "%.1f".format(rating)
            saveBook()
        }

        binding.statusDropdown.setOnItemClickListener { _, _, position, _ ->
            currentBook.status = BookStatus.values()[position]
            saveBook()
        }

        binding.btnAddSummary.setOnClickListener {
            binding.summaryContainer.visibility = if (binding.summaryContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.etBookSummary.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                currentBook.summary = binding.etBookSummary.text.toString()
                saveBook()
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun saveReadingProgress() {
        val pagesReadText = binding.etPagesRead.text.toString()
        if (pagesReadText.isBlank()) {
            Toast.makeText(this, "Введите количество страниц", Toast.LENGTH_SHORT).show()
            return
        }

        val pagesRead = pagesReadText.toIntOrNull() ?: run {
            Toast.makeText(this, "Введите число", Toast.LENGTH_SHORT).show()
            return
        }

        if (pagesRead < 0 || pagesRead > currentBook.totalPages) {
            Toast.makeText(this, "Некорректное количество страниц", Toast.LENGTH_SHORT).show()
            return
        }

        currentBook.readPages = pagesRead
        saveBook()
        updatePagesInfo()

        binding.btnSaveProgress.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                binding.btnSaveProgress.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun updatePagesInfo() {
        binding.tvPagesLeft.text = "Осталось: ${currentBook.totalPages - currentBook.readPages} страниц"
        binding.progressBarReading.progress = currentBook.readPages

        when {
            currentBook.readPages == 0 -> currentBook.status = BookStatus.PLANNED
            currentBook.readPages == currentBook.totalPages -> currentBook.status = BookStatus.FINISHED
            else -> currentBook.status = BookStatus.READING
        }
        binding.statusDropdown.setText(currentBook.status.toString(), false)
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
        binding.etPagesRead.setText(currentBook.readPages.toString())
    }
}