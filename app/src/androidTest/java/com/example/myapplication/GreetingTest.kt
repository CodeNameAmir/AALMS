package com.example.myapplication

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test

class GreetingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun greeting_displaysCorrectText() {
        // Start the app
        composeTestRule.setContent {
            MyApplicationTheme {
                Greeting(name = "Android")
            }
        }

        // Check if the text "Hello Android!" is displayed
        composeTestRule.onNodeWithText("Hello Android!").assertIsDisplayed()
    }
}
