package com.example.unitime.ui

import android.app.Activity
import android.content.Intent
import com.example.unitime.R
import com.example.unitime.ui.auth.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavigationHelper {

    fun setup(activity: Activity, selectedItemId: Int) {
        val bottomNavigation =
            activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Сбрасываем слушатель, чтобы не копились
        bottomNavigation.setOnItemSelectedListener(null)

        // ⭐ Принудительно обновляем checked у всех пунктов меню
        // (иначе при REORDER_TO_FRONT визуал не обновляется)
        val menu = bottomNavigation.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            item.isChecked = (item.itemId == selectedItemId)
        }

        bottomNavigation.selectedItemId = selectedItemId

        bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val targetActivity = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_profile -> ProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            activity.startActivity(
                Intent(activity, targetActivity).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
            )
            activity.overridePendingTransition(0, 0)

            true
        }
    }
}