package com.example.readers_diary

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.Context.MODE_PRIVATE
import com.example.readerdiary.Book

class BookRepositoryImpl(
    private val context: Context,
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("books", Context.MODE_PRIVATE)
) : BookRepository {

    private val gson = Gson()
    private val bookListType = object : TypeToken<List<Book>>() {}.type

    override fun saveBooks(books: List<Book>) {
        sharedPreferences.edit {
            putString("books_data", gson.toJson(books))
        }
    }

    override fun loadBooks(): List<Book> {
        return sharedPreferences.getString("books_data", null)?.let { json ->
            try {
                gson.fromJson(json, bookListType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
}