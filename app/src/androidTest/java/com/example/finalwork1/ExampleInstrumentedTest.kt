package com.example.finalwork1

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PersonalStatusFilterUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dislikeFilterIsVisibleAndSelectable() {
        val dislikeFilter = composeRule.onNodeWithText("Не нравится")
        dislikeFilter.assertExists()
        dislikeFilter.performClick()
        dislikeFilter.assertIsSelected()
    }
}
