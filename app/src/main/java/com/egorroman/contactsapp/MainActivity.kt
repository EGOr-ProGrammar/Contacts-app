package com.egorroman.contactsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.egorroman.contactsapp.presentation.ContactsScreen
import com.egorroman.contactsapp.ui.theme.ContactsAppTheme
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object ContactsList : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ContactsAppTheme {
                val backStack = rememberNavBackStack(Screen.ContactsList)

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<Screen.ContactsList> {
                            ContactsScreen()
                        }
                    }
                )
            }
        }
    }
}
