package com.example.readers_diary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.readerdiary.Book
import com.example.readerdiary.BookStatus

class BookAdapter(
    private var books: List<Book>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.bookTitle)
        private val author: TextView = view.findViewById(R.id.bookAuthor)
        private val pages: TextView = view.findViewById(R.id.bookPages)
        private val status: TextView = view.findViewById(R.id.bookStatus)
        private val rating: RatingBar = view.findViewById(R.id.bookRating)

        fun bind(book: Book) {
            title.text = book.title
            author.text = "Автор: ${book.author}"
            pages.text = "Прочитано: ${book.readPages}/${book.totalPages} стр."
            status.text = when (book.status) {
                BookStatus.FINISHED -> "Прочитано"
                BookStatus.READING -> "Читаю"
                BookStatus.PLANNED -> "В планах"
            }
            rating.rating = book.rating

            itemView.setOnClickListener { onItemClick(book) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int {
        return books.size
    }

    fun updateList(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged() // Уведомляем адаптер об изменении данных
    }
}