package com.example.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.di.AppContainer
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.main.MainAssistantScreen
import com.example.ui.screens.main.MainViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.splash.SplashScreen

@Composable
fun GamakNavHost(
  navController: NavHostController,
  container: AppContainer,
  modifier: Modifier = Modifier
) {
  NavHost(
    navController = navController,
    startDestination = Screen.Splash.route,
    modifier = modifier
  ) {
    composable(
      route = Screen.Splash.route,
      exitTransition = {
        fadeOut(animationSpec = tween(400))
      }
    ) {
      SplashScreen(
        onSplashFinished = {
          navController.navigate(Screen.Main.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
          }
        }
      )
    }

    composable(
      route = Screen.Main.route,
      enterTransition = {
        fadeIn(animationSpec = tween(400))
      },
      exitTransition = {
        slideOutOfContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Left,
          animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
      },
      popEnterTransition = {
        slideIntoContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
      }
    ) {
      val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(
          assistantEngine = container.assistantEngine,
          settingsRepository = container.settingsRepository
        )
      )

      MainAssistantScreen(
        viewModel = mainViewModel,
        onNavigateToSettings = {
          navController.navigate(Screen.Settings.route)
        },
        onNavigateToAbout = {
          navController.navigate(Screen.About.route)
        }
      )
    }

    composable(
      route = Screen.Settings.route,
      enterTransition = {
        slideIntoContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Left,
          animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
      },
      exitTransition = {
        slideOutOfContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
      },
      popExitTransition = {
        slideOutOfContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
      }
    ) {
      val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
          settingsRepository = container.settingsRepository,
          wakeWordEngine = container.wakeWordEngine,
          assistantEngine = container.assistantEngine,
          memoryRepository = container.memoryRepository,
          assistantAudioManager = container.assistantAudioManager
        )
      )

      SettingsScreen(
        viewModel = settingsViewModel,
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }

    composable(
      route = Screen.About.route,
      enterTransition = {
        slideIntoContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Left,
          animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
      },
      popExitTransition = {
        slideOutOfContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
      }
    ) {
      AboutScreen(
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }
  }
}
