package com.example.readers_diary

import BookAdapter
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.readerdiary.Book
import com.google.android.material.snackbar.Snackbar
import com.example.readerdiary.BookStatus


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookAdapter
    private lateinit var searchEditText: EditText
    private lateinit var buttonRead: Button
    private lateinit var buttonInProcess: Button
    private lateinit var buttonPlanned: Button
    private lateinit var buttonAddBook: ImageButton
    private lateinit var buttonListBooks: Button
    private lateinit var buttonStatistics: Button

    private var bookList = mutableListOf<Book>()
    private var filteredList = mutableListOf<Book>()
    private var currentFilter = BookFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        loadBooks()
        setupListeners()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewBooks)
        searchEditText = findViewById(R.id.editTextSearch)
        buttonRead = findViewById(R.id.buttonRead)
        buttonInProcess = findViewById(R.id.buttonInProcess)
        buttonPlanned = findViewById(R.id.buttonPlanned)
        buttonAddBook = findViewById(R.id.buttonAddBook)
        buttonListBooks = findViewById(R.id.buttonListBooks)
        buttonStatistics = findViewById(R.id.buttonStatistics)
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter(filteredList) { book ->
            openBookDetails(book)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadBooks() {
        // Здесь должна быть загрузка из SharedPreferences/файлов

        filterBooks(currentFilter)
    }

    private fun setupListeners() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterBooks(currentFilter, s.toString())
            }
        })

        buttonRead.setOnClickListener { filterBooks(BookFilter.FINISHED) }
        buttonInProcess.setOnClickListener { filterBooks(BookFilter.READING) }
        buttonPlanned.setOnClickListener { filterBooks(BookFilter.PLANNED) }

        buttonAddBook.setOnClickListener {
            val intent = Intent(this, AddBookActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        buttonListBooks.setOnClickListener {
            filterBooks(BookFilter.ALL)
            Snackbar.make(recyclerView, "Показаны все книги", Snackbar.LENGTH_SHORT).show()
        }

        buttonStatistics.setOnClickListener {
            openStatisticsScreen()
        }
    }

    private fun filterBooks(filter: BookFilter, searchQuery: String = "") {
        currentFilter = filter
        filteredList.clear()

        when (filter) {
            BookFilter.ALL -> filteredList.addAll(bookList)
            BookFilter.FINISHED -> filteredList.addAll(bookList.filter { it.status == BookStatus.FINISHED })
            BookFilter.READING -> filteredList.addAll(bookList.filter { it.status == BookStatus.READING })
            BookFilter.PLANNED -> filteredList.addAll(bookList.filter { it.status == BookStatus.PLANNED })
        }

        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.contains(searchQuery, true) || it.author.contains(searchQuery, true)
            }.toMutableList()
        }

        updateFilterButtons()
        adapter.updateList(filteredList)
    }

    private fun updateFilterButtons() {
        val selectedColor = getColor(android.R.color.holo_blue_dark)
        val normalColor = getColor(android.R.color.holo_blue_light)

        buttonRead.setBackgroundColor(if (currentFilter == BookFilter.FINISHED) selectedColor else normalColor)
        buttonInProcess.setBackgroundColor(if (currentFilter == BookFilter.READING) selectedColor else normalColor)
        buttonPlanned.setBackgroundColor(if (currentFilter == BookFilter.PLANNED) selectedColor else normalColor)
    }

    private fun openBookDetails(book: Book) {
        val intent = Intent(this, BookDetailsActivity::class.java).apply {
            putExtra("BOOK_ID", book.id)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openStatisticsScreen() {
        val intent = Intent(this, StatisticsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}

enum class BookFilter {
    ALL, FINISHED, READING, PLANNED
}


