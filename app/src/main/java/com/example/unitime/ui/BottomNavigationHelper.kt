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

        bottomNavigation.selectedItemId = selectedItemId

        bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    activity.startActivity(
                        Intent(activity, MainActivity::class.java)
                    )

                    activity.overridePendingTransition(
                        R.anim.slide_to_right,
                        R.anim.slide_to_left
                    )

                    true
                }

                R.id.nav_profile -> {
                    activity.startActivity(
                        Intent(activity, ProfileActivity::class.java)
                    )

                    activity.overridePendingTransition(
                        R.anim.slide_to_right,
                        R.anim.slide_to_left
                    )

                    true
                }

                R.id.nav_activities -> {
                    // Здесь позже добавим переход к моим активностям
                    true
                }

                else -> false
            }
        }
    }
}