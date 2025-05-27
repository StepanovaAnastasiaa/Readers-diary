package com.example.readers_diary

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.readerdiary.Book
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var totalBooksTextView: TextView
    private lateinit var totalPagesTextView: TextView
    private lateinit var averagePagesTextView: TextView
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var statisticsTableLayout: TableLayout
    private lateinit var bookRepository: BookRepository

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        bookRepository = BookRepositoryImpl(this)
        initViews()
        setupListeners()
        loadStatistics()
        setupBottomMenu()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем статистику при возвращении на экран
        loadStatistics()
    }

    private fun initViews() {
        totalBooksTextView = findViewById(R.id.TotalBooks)
        totalPagesTextView = findViewById(R.id.TotalPages)
        averagePagesTextView = findViewById(R.id.AveragePages)
        startDateEditText = findViewById(R.id.PerStartDate)
        endDateEditText = findViewById(R.id.PerEndDate)
        searchButton = findViewById(R.id.buttonSearch)
        statisticsTableLayout = findViewById(R.id.tableStatistics)
    }

    private fun setupListeners() {
        startDateEditText.setOnClickListener { showDatePickerDialog(true) }
        endDateEditText.setOnClickListener { showDatePickerDialog(false) }
        searchButton.setOnClickListener { loadStatistics() }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                if (isStartDate) {
                    startDateEditText.setText(dateFormat.format(calendar.time))
                } else {
                    endDateEditText.setText(dateFormat.format(calendar.time))
                }
                loadStatistics() // Автоматически обновляем статистику при выборе даты
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadStatistics() {
        val books = if (startDateEditText.text.isNotEmpty() && endDateEditText.text.isNotEmpty()) {
            val startDate = dateFormat.parse(startDateEditText.text.toString())
            val endDate = dateFormat.parse(endDateEditText.text.toString())

            if (startDate != null && endDate != null) {
                bookRepository.loadBooks().filter {
                    it.addedDate in startDate.time..endDate.time
                }
            } else {
                bookRepository.loadBooks()
            }
        } else {
            bookRepository.loadBooks()
        }

        updateStatistics(books)
    }

    private fun updateStatistics(books: List<Book>) {
        // Общая статистика
        val totalBooks = books.size
        val totalPages = books.sumOf { it.readPages }

        // Рассчитываем количество дней чтения
        val readingDays = calculateReadingDays(books)
        val averagePagesPerDay = if (readingDays > 0) totalPages / readingDays else 0

        totalBooksTextView.text = "Книг: $totalBooks"
        totalPagesTextView.text = "Страниц: $totalPages"
        averagePagesTextView.text = "Средне в день: $averagePagesPerDay"

        // Статистика по дням
        updateStatisticsTable(books)
    }

    private fun calculateReadingDays(books: List<Book>): Int {
        if (books.isEmpty()) return 0

        // Находим самую раннюю и позднюю даты чтения
        val minDate = books.minOf { it.addedDate }
        val maxDate = books.maxOf { it.addedDate }

        // Вычисляем разницу в днях
        return ((maxDate - minDate) / (1000 * 60 * 60 * 24)).toInt() + 1
    }

    private fun updateStatisticsTable(books: List<Book>) {
        // Очищаем таблицу, кроме заголовков
        while (statisticsTableLayout.childCount > 1) {
            statisticsTableLayout.removeViewAt(1)
        }

        // Группируем книги по датам
        val booksByDate = books.groupBy {
            dateFormat.format(Date(it.addedDate))
        }

        // Сортируем даты
        booksByDate.keys.sortedBy { dateFormat.parse(it)?.time ?: 0 }.forEach { date ->
            val dayBooks = booksByDate[date] ?: return@forEach
            val totalDayPages = dayBooks.sumOf { it.readPages }

            val tableRow = TableRow(this).apply {
                addView(TextView(this@StatisticsActivity).apply {
                    text = date
                    setTextColor(resources.getColor(android.R.color.black))
                    setPadding(8, 8, 8, 8)
                })
                addView(TextView(this@StatisticsActivity).apply {
                    text = totalDayPages.toString()
                    setTextColor(resources.getColor(android.R.color.black))
                    setPadding(8, 8, 8, 8)
                })
            }

            statisticsTableLayout.addView(tableRow)
        }
    }

    private fun setupBottomMenu() {
        findViewById<Button>(R.id.buttonListBooks).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.buttonStatistics).setOnClickListener {
            // Обновляем статистику при повторном нажатии
            loadStatistics()
        }
    }
}