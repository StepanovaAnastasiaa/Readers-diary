package com.example.readers_diary

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.readerdiary.Book
import com.google.android.material.snackbar.Snackbar
import com.example.readerdiary.BookStatus

class MainActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookAdapter
    private lateinit var searchEditText: EditText
    private lateinit var buttonRead: Button
    private lateinit var buttonInProcess: Button
    private lateinit var buttonPlanned: Button
    private lateinit var buttonAddBook: ImageButton
    private lateinit var buttonListBooks: Button
    private lateinit var buttonStatistics: Button
    private lateinit var textEmptyState: TextView

    // Данные
    private var bookList = mutableListOf<Book>()
    private var filteredList = mutableListOf<Book>()
    private var currentFilter = BookFilter.ALL
    private lateinit var bookRepository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация репозитория для хранения книг
        bookRepository = BookRepositoryImpl(this)

        initViews()
        setupRecyclerView()
        loadBooks()
        setupListeners()
        setupBottomMenu()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        loadBooks()
    }
    private val addBookLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadBooks() // Обновляем список
        }
    }



    private fun initViews() {
        // Находим все View элементы
        recyclerView = findViewById(R.id.recyclerViewBooks)
        searchEditText = findViewById(R.id.editTextSearch)
        buttonRead = findViewById(R.id.buttonRead)
        buttonInProcess = findViewById(R.id.buttonInProcess)
        buttonPlanned = findViewById(R.id.buttonPlanned)
        buttonAddBook = findViewById(R.id.buttonAddBook)
        buttonListBooks = findViewById(R.id.buttonListBooks)
        buttonStatistics = findViewById(R.id.buttonStatistics)
        textEmptyState = findViewById(R.id.textEmptyState)
    }

    private fun setupRecyclerView() {
        // Настраиваем RecyclerView и адаптер
        adapter = BookAdapter(filteredList) { book ->
            openBookDetails(book)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadBooks() {
        // Загружаем книги из хранилища
        bookList.clear()
        bookList.addAll(bookRepository.loadBooks())

        // Если список пуст, добавляем примеры книг (для тестирования)
        if (bookList.isEmpty()) {
            bookList.addAll(getSampleBooks())
            bookRepository.saveBooks(bookList)
            Snackbar.make(recyclerView, "Добавлены примеры книг", Snackbar.LENGTH_SHORT).show()
        }

        filterBooks(currentFilter)
    }

    private fun getSampleBooks(): List<Book> {
        // Создаем несколько книг для примера
        return listOf(
            Book(
                title = "Война и мир",
                author = "Лев Толстой",
                totalPages = 1225,
                readPages = 300,
                status = BookStatus.READING,
                rating = 4.5f,
                summary = "Роман-эпопея о русском обществе во время войн с Наполеоном"
            ),
            Book(
                title = "1984",
                author = "Джордж Оруэлл",
                totalPages = 328,
                readPages = 328,
                status = BookStatus.FINISHED,
                rating = 5f,
                summary = "Классическая антиутопия о тоталитарном обществе"
            ),
            Book(
                title = "Гарри Поттер и философский камень",
                author = "Дж. К. Роулинг",
                totalPages = 400,
                readPages = 0,
                status = BookStatus.PLANNED,
                rating = 0f,
                summary = "Первая книга о юном волшебнике Гарри Поттере"
            )
        )
    }

    private fun setupListeners() {
        // Поиск книг при изменении текста
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterBooks(currentFilter, s.toString())
            }
        })

        // Кнопки фильтрации
        buttonRead.setOnClickListener {
            filterBooks(BookFilter.FINISHED)
            searchEditText.text.clear()
        }
        buttonInProcess.setOnClickListener {
            filterBooks(BookFilter.READING)
            searchEditText.text.clear()
        }
        buttonPlanned.setOnClickListener {
            filterBooks(BookFilter.PLANNED)
            searchEditText.text.clear()
        }

        // Запуск активности:
        buttonAddBook.setOnClickListener {
            addBookLauncher.launch(Intent(this, AddBookActivity::class.java))
        }
    }

    private fun setupBottomMenu() {
        // Кнопка "Список книг" (текущая страница)
        buttonListBooks.setOnClickListener {
            // Ничего не делаем, так как мы уже на главном экране
        }

        // Кнопка "Статистика"
        buttonStatistics.setOnClickListener {
            Log.d("MainActivity", "Statistics button clicked")
            val intent = Intent(this@MainActivity, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun filterBooks(filter: BookFilter, searchQuery: String = "") {
        currentFilter = filter
        filteredList.clear()

        // Фильтрация по статусу
        when (filter) {
            BookFilter.ALL -> filteredList.addAll(bookList)
            BookFilter.FINISHED -> filteredList.addAll(bookList.filter { it.status == BookStatus.FINISHED })
            BookFilter.READING -> filteredList.addAll(bookList.filter { it.status == BookStatus.READING })
            BookFilter.PLANNED -> filteredList.addAll(bookList.filter { it.status == BookStatus.PLANNED })
        }

        // Дополнительная фильтрация по поисковому запросу
        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.contains(searchQuery, true) ||
                        it.author.contains(searchQuery, true) ||
                        it.summary.contains(searchQuery, true)
            }.toMutableList()
        }

        updateFilterButtons()
        adapter.updateList(filteredList)

        // Показываем сообщение, если книг не найдено
        if (filteredList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        textEmptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        textEmptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun updateFilterButtons() {
        // Обновляем цвета кнопок фильтрации
        val selectedColor = getColor(android.R.color.holo_blue_dark)
        val normalColor = getColor(android.R.color.holo_blue_light)

        buttonRead.setBackgroundColor(if (currentFilter == BookFilter.FINISHED) selectedColor else normalColor)
        buttonInProcess.setBackgroundColor(if (currentFilter == BookFilter.READING) selectedColor else normalColor)
        buttonPlanned.setBackgroundColor(if (currentFilter == BookFilter.PLANNED) selectedColor else normalColor)
    }

    private fun openBookDetails(book: Book) {
        // Открываем экран с деталями книги
        val intent = Intent(this@MainActivity, BookDetailsActivity::class.java).apply {
            putExtra("BOOK_ID", book.id)
        }
        startActivity(intent)
    }
}

// Перечисление для фильтров
enum class BookFilter {
    ALL, FINISHED, READING, PLANNED
}