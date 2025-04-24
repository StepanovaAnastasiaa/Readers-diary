package com.example.readers_diary

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.readerdiary.Book
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {
    // UI элементы
    private lateinit var totalBooksTextView: TextView
    private lateinit var totalPagesTextView: TextView
    private lateinit var averagePagesTextView: TextView
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var statisticsTableLayout: TableLayout

    // Репозиторий для работы с книгами
    private lateinit var bookRepository: BookRepository

    // Календарь для выбора дат
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Инициализация репозитория
        bookRepository = BookRepositoryImpl(this)

        // Инициализация UI элементов
        initViews()

        // Настройка слушателей
        setupListeners()

        // Загрузка общей статистики
        loadOverallStatistics()

        // Настройка нижнего меню
        setupBottomMenu()
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
        // Установка слушателей для выбора дат
        startDateEditText.setOnClickListener { showDatePickerDialog(true) }
        endDateEditText.setOnClickListener { showDatePickerDialog(false) }

        // Слушатель для кнопки поиска
        searchButton.setOnClickListener { searchStatistics() }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                if (isStartDate) {
                    startDateEditText.setText(sdf.format(calendar.time))
                } else {
                    endDateEditText.setText(sdf.format(calendar.time))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun loadOverallStatistics() {
        val books = bookRepository.loadBooks()

        // Общее количество книг
        val totalBooks = books.size
        totalBooksTextView.text = "Книг: $totalBooks"

        // Общее количество прочитанных страниц
        val totalPages = books.sumOf { it.readPages }
        totalPagesTextView.text = "Страниц: $totalPages"

        // Средние страницы в день (примерная оценка)
        val averagePages = if (totalBooks > 0) totalPages / totalBooks else 0
        averagePagesTextView.text = "В среднем за день: $averagePages"

        // Обновляем таблицу статистики по всем книгам
        updateStatisticsTable(books)
    }

    private fun searchStatistics() {
        val startDateStr = startDateEditText.text.toString()
        val endDateStr = endDateEditText.text.toString()

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
            // Если даты не выбраны, показываем общую статистику
            loadOverallStatistics()
            return
        }

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val startDate = sdf.parse(startDateStr)
        val endDate = sdf.parse(endDateStr)

        if (startDate != null && endDate != null) {
            // Фильтрация книг по датам
            val books = bookRepository.loadBooks()
                .filter { book ->
                    book.addedDate in startDate.time..endDate.time
                }

            updateStatisticsTable(books)
        }
    }

    private fun updateStatisticsTable(books: List<Book>) {
        // Очистка таблицы (оставляем только заголовок)
        while (statisticsTableLayout.childCount > 1) {
            statisticsTableLayout.removeViewAt(1)
        }

        // Группировка книг по дням
        val booksByDate = books.groupBy {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it.addedDate))
        }

        // Сортировка дат
        val sortedDates = booksByDate.keys.sortedBy {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it)
        }

        // Добавление данных о книгах в таблицу
        for (date in sortedDates)
        // Добавление данных о книгах в таблицу
            for (date in sortedDates) {
                val dayBooks = booksByDate[date] ?: continue
                val totalDayPages = dayBooks.sumOf { it.readPages }

                val tableRow = android.widget.TableRow(this)

                val dateTextView = android.widget.TextView(this).apply {
                    text = date
                    setPadding(8, 8, 8, 8)
                }

                val pagesTextView = android.widget.TextView(this).apply {
                    text = totalDayPages.toString()
                    setPadding(8, 8, 8, 8)
                }

                tableRow.addView(dateTextView)
                tableRow.addView(pagesTextView)

                statisticsTableLayout.addView(tableRow)
            }
    }

    private fun setupBottomMenu() {
        // Кнопка "Список книг"
        findViewById<Button>(R.id.buttonListBooks).setOnClickListener {
            val intent = Intent(this@StatisticsActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Кнопка "Статистика" (текущая страница)
        findViewById<Button>(R.id.buttonStatistics).setOnClickListener {
            // Ничего не делаем, так как мы уже на странице статистики
        }
    }
}