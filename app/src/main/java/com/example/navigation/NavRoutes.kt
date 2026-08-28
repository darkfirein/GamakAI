package com.example.navigation

sealed class Screen(val route: String) {
  data object Splash : Screen("splash")
  data object Main : Screen("main")
  data object Settings : Screen("settings")
  data object About : Screen("about")
}
