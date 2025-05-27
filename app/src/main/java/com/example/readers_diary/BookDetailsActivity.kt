package com.example.readers_diary

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.readerdiary.Book
import com.example.readerdiary.BookStatus
import com.example.readers_diary.databinding.ReadBinding
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.app.AlertDialog

class BookDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ReadBinding
    private lateinit var currentBook: Book
    private lateinit var bookRepository: BookRepository

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookRepository = BookRepositoryImpl(applicationContext)

        val bookId = intent.getStringExtra("BOOK_ID") ?: run {
            finish()
            return
        }

        currentBook = bookRepository.loadBooks().firstOrNull { it.id == bookId }?.copy() ?: run {
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
        updateProgressBar()

        binding.ratingBar.rating = currentBook.rating
        binding.tvRatingValue.text = "%.1f".format(currentBook.rating)

        setupStatusDropdown()

        binding.etBookSummary.setText(currentBook.summary)

        loadCoverImage()

        // Обновляем состояние кнопки краткого содержания
        // Инициализация состояния контейнера с кратким содержанием
        if (currentBook.summary.isNotEmpty()) {
            binding.summaryContainer.visibility = View.VISIBLE
            binding.btnAddSummary.text = "Скрыть краткое содержание"
            binding.etBookSummary.setText(currentBook.summary)
        } else {
            binding.summaryContainer.visibility = View.GONE
            binding.btnAddSummary.text = "Добавить краткое содержание"
        }

        // Делаем кнопку всегда активной
        binding.btnAddSummary.isEnabled = true
    }

    private fun setupStatusDropdown() {
        val statuses = BookStatus.values().map { getStatusDisplayName(it) }
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, statuses)
        binding.statusDropdown.setAdapter(adapter)
        binding.statusDropdown.setText(getStatusDisplayName(currentBook.status), false)
    }

    private fun deleteBook() {
        try {
            val books = bookRepository.loadBooks().toMutableList()
            books.removeAll { it.id == currentBook.id }
            bookRepository.saveBooks(books)

            // Delete cover image if exists
            currentBook.coverImagePath?.let { path ->
                File(path).delete()
            }

            Toast.makeText(this, "Книга удалена", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при удалении книги", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удаление книги")
            .setMessage("Вы уверены, что хотите удалить эту книгу? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteBook()
            }
            .setNegativeButton("Отмена", null)
            .create()
            .show()
    }

    private fun getStatusDisplayName(status: BookStatus): String {
        return when (status) {
            BookStatus.PLANNED -> "В планах"
            BookStatus.READING -> "Читаю"
            BookStatus.FINISHED -> "Прочитано"
        }
    }

    private fun setupListeners() {
        binding.btnSaveProgress.setOnClickListener {
            saveReadingProgress()
        }
        binding.btnDeleteBook.setOnClickListener {
            showDeleteConfirmationDialog()
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
        binding.etBookSummary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Автосохранение при изменении текста
                currentBook.summary = s.toString()
            }
        })
        binding.btnAddSummary.setOnClickListener {
            toggleSummaryVisibility()
            currentBook.summary = binding.etBookSummary.text.toString()
            saveBook()
        }

        binding.etBookSummary.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                currentBook.summary = binding.etBookSummary.text.toString()
                saveBook()
            }
        }

        binding.btnBack.setOnClickListener {
            saveBookAndFinish()
        }

        binding.btnAddCover.setOnClickListener {
            openImagePicker()
        }
    }

    private fun toggleSummaryVisibility() {
        if (binding.summaryContainer.visibility == View.VISIBLE) {
            binding.summaryContainer.visibility = View.GONE
            binding.btnAddSummary.text = "Добавить краткое содержание"
            // Сохраняем текст перед скрытием
            currentBook.summary = binding.etBookSummary.text.toString()
            saveBook()
        } else {
            binding.summaryContainer.visibility = View.VISIBLE
            binding.btnAddSummary.text = "Скрыть краткое содержание"
            binding.etBookSummary.requestFocus()
            // Если текст пустой, устанавливаем плейсхолдер
            if (binding.etBookSummary.text.isNullOrEmpty()) {
                binding.etBookSummary.hint = "Введите краткое содержание книги..."
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val currentBooks = bookRepository.loadBooks().toMutableList()
            val bookIndex = currentBooks.indexOfFirst { it.id == currentBook.id }
            if (bookIndex == -1) return

            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val coversDir = File(filesDir, "book_covers").apply { mkdirs() }
            val coverFile = File(coversDir, "${currentBook.id}.jpg")
            FileOutputStream(coverFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }

            currentBook.coverImageUri = uri.toString()
            currentBook.coverImagePath = coverFile.absolutePath

            currentBooks[bookIndex] = currentBook
            bookRepository.saveBooks(currentBooks)

            runOnUiThread {
                binding.ivBookCover.setImageBitmap(bitmap)
                binding.ivBookCover.visibility = View.VISIBLE
                Toast.makeText(this, "Обложка сохранена", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Ошибка при загрузке изображения", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
    }

    private fun loadCoverImage() {
        currentBook.coverImagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.ivBookCover.setImageBitmap(bitmap)
                binding.ivBookCover.visibility = View.VISIBLE
                return
            }
        }

        currentBook.coverImageUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                binding.ivBookCover.setImageBitmap(bitmap)
                binding.ivBookCover.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        if (pagesRead < 0 || pagesRead > currentBook.totalPages-currentBook.readPages) {
            Toast.makeText(this, "Некорректное количество страниц", Toast.LENGTH_SHORT).show()
            return
        }

        currentBook.readPages += pagesRead
        currentBook.lastUpdated = System.currentTimeMillis()
        updateBookStatus()
        saveBook()
        updatePagesInfo()
        updateProgressBar()

        showSaveAnimation()
    }

    private fun updateBookStatus() {
        currentBook.status = when {
            currentBook.readPages == 0 -> BookStatus.PLANNED
            currentBook.readPages >= currentBook.totalPages -> BookStatus.FINISHED
            else -> BookStatus.READING
        }
        binding.statusDropdown.setText(getStatusDisplayName(currentBook.status), false)
    }

    private fun updatePagesInfo() {
        binding.tvPagesLeft.text = "Осталось: ${currentBook.pagesLeft} ${getPageWord(currentBook.pagesLeft)}"
    }

    private fun updateProgressBar() {
        binding.progressBarReading.max = currentBook.totalPages
        binding.progressBarReading.progress = currentBook.readPages
    }

    private fun getPageWord(count: Int): String {
        return when {
            count % 100 in 11..14 -> "страниц"
            count % 10 == 1 -> "страница"
            count % 10 in 2..4 -> "страницы"
            else -> "страниц"
        }
    }

    private fun showSaveAnimation() {
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

    private fun saveBook() {
        try {
            val books = bookRepository.loadBooks().toMutableList()
            val index = books.indexOfFirst { it.id == currentBook.id }
            if (index != -1) {
                books[index] = currentBook.copy(
                    title = currentBook.title,
                    author = currentBook.author,
                    totalPages = currentBook.totalPages,
                    readPages = currentBook.readPages,
                    status = currentBook.status,
                    rating = currentBook.rating,
                    summary = binding.etBookSummary.text.toString(),
                    lastUpdated = System.currentTimeMillis(),
                    coverImageUri = currentBook.coverImageUri,
                    coverImagePath = currentBook.coverImagePath
                )
                bookRepository.saveBooks(books)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun saveBookAndFinish() {
        currentBook.summary = binding.etBookSummary.text.toString()
        saveBook()
        finish()
    }

    private fun updateUI() {
        binding.etPagesRead.setText(currentBook.readPages.toString())
    }
}