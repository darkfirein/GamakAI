package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.GamakTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun gamak_splash_screenshot() {
    composeTestRule.setContent {
      GamakTheme {
        SplashScreen(onSplashFinished = {})
      }
    }

    val screenshotFile = File("src/test/screenshots/greeting.png")
    screenshotFile.parentFile?.mkdirs()
    composeTestRule.onRoot().captureRoboImage(filePath = screenshotFile.path)
  }
}

