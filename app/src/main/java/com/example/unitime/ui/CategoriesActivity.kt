package com.example.unitime.ui.categories

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unitime.R
import com.example.unitime.ui.CategoryAdapter

class CategoriesActivity : AppCompatActivity() {

    private val allCategories = listOf(
        Category(R.drawable.cup,     "Спорт",         "спорт"),
        Category(R.drawable.music,      "Музыка",        "музыка"),
        Category(R.drawable.notebook,       "Учёба",         "учёба"),
        Category(R.drawable.food,       "Еда",           "еда"),
        Category(R.drawable.gamepad,       "Игры",          "игры"),
        Category(R.drawable.gallery,        "Искусство",     "искусство"),
        Category(R.drawable.volonter,  "Волонтёрство",  "волонтёрство"),
        Category(R.drawable.users,      "Встречи",       "встречи"),
        Category(R.drawable.star,       "Другое",        "другое")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val recycler = findViewById<RecyclerView>(R.id.categoriesRecycler)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = CategoryAdapter(allCategories) { category ->
            val result = Intent().putExtra("tagName", category.tagName)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }
}