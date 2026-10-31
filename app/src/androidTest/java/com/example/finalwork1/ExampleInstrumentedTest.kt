
package com.example.finalwork1

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
        composeRule.onNodeWithText("Личный статус: Все")
            .performClick()

        composeRule.onNodeWithText("Не нравится")
            .assertExists()
            .performClick()

        composeRule.onNodeWithText("Личный статус: Не нравится")
            .assertExists()
    }
}
