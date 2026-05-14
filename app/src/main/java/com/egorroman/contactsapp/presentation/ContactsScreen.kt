package com.egorroman.contactsapp.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.egorroman.contactsapp.domain.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    var pendingCallContact by remember { mutableStateOf<Contact?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGiven ->
        if (isGiven) {
            viewModel.loadContacts(context.contentResolver)
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCallContact?.let { contact ->
                val intent = Intent(Intent.ACTION_CALL, "tel:${contact.phone}".toUri())
                context.startActivity(intent)
            }
        }
        pendingCallContact = null
    }

    val makeCall = { contact: Contact ->
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL, "tel:${contact.phone}".toUri())
            context.startActivity(intent)
        } else {
            pendingCallContact = contact
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            viewModel.loadContacts(context.contentResolver)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ContactsTopAppBar(scrollBehavior = scrollBehavior)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                state.isLoading -> {
                    LoadingScreen()
                }

                state.isPermissionRequired -> {
                    PermissionRationaleScreen(
                        onGrantClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    )
                }

                state.errorMessage != null -> {
                    ErrorScreen(message = state.errorMessage!!)
                }

                else -> {
                    if (state.contacts.isEmpty()) {
                        EmptyState()
                    } else {
                        ContactsList(
                            groupedContacts = state.contacts,
                            onContactClick = makeCall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        title = {
            Text(
                text = "Контакты",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = Color.Unspecified,
            titleContentColor = Color.Unspecified,
            actionIconContentColor = Color.Unspecified
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsList(
    groupedContacts: Map<Char, List<Contact>>,
    onContactClick: (Contact) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        groupedContacts.forEach { (initial, contacts) ->
            stickyHeader {
                ContactHeader(initial = initial)
            }
            items(contacts, key = { it.id + it.phone }) { contact ->
                ContactItem(contact = contact, onClick = { onContactClick(contact) })
            }
        }
    }
}

@Composable
fun ContactHeader(initial: Char) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Text(
            text = initial.toString(),
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(initial = contact.initial)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${contact.firstName} ${contact.lastName}".trim(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ContactAvatar(initial: Char) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun PermissionRationaleScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Требуется доступ к контактам",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Нужно разрешение на чтение ваших контактов, чтобы отобразить их в списке. Без этого приложение не сможет работать.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGrantClick,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = "Предоставить разрешение")
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Контакты не найдены",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
