package com.cv.leafavoapp.ui.navigation

sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Tutorial : Screen("tutorial")
    object Developer : Screen("developer")
    object Detection : Screen("detection")
    object DetailScreen : Screen("home/{leafavoId}") {
        fun createRoute(leafavoId: Long) = "home/$leafavoId"
    }
}