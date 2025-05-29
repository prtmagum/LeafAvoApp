package com.cv.leafavoapp.ui.onboarding

import android.content.Context

class TutorialUtils(private val context: Context) {

    fun isTutorialCompleted(): Boolean {
        return context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            .getBoolean("completed", false)
    }

    fun setTutorialCompleted() {
        context.getSharedPreferences("tutorial", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("completed", true)
            .apply()
    }

}