package com.example.readers_diary

import com.example.readerdiary.Book

interface BookRepository {
    fun saveBooks(books: List<Book>)
    fun loadBooks(): List<Book>
}