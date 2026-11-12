package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.CloukeEmerald
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationShell(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    userEmail: String,
    onSignOut: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showUserMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // Tablet / Desktop Layout: Permanent Navigation Rail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .testTag("app_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    header = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0D0D11),
                            modifier = Modifier
                                .size(40.dp)
                                .padding(top = 4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "CLOUKE",
                                    tint = CloukeEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    for (dest in AppDestination.entries) {
                        NavigationRailItem(
                            selected = currentDestination == dest,
                            onClick = { onNavigate(dest) },
                            icon = { Icon(dest.icon, contentDescription = dest.title) },
                            label = { Text(dest.shortLabel, fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("rail_item_${dest.name.lowercase()}")
                        )
                    }
                }

                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.5.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Scaffold(
                    topBar = {
                        CloukeTopBar(
                            currentDestination = currentDestination,
                            userEmail = userEmail,
                            onMenuClick = null,
                            onSignOut = onSignOut
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    content(innerPadding)
                }
            }
        } else {
            // Mobile Phone Layout: Drawer + Bottom Bar
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .width(280.dp)
                            .testTag("mobile_drawer_sheet"),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D0D11),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CloukeEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CLOUKE AI",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Commercial IDE Platform",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                        for (dest in AppDestination.entries) {
                            NavigationDrawerItem(
                                label = { Text(dest.title, fontSize = 13.sp) },
                                icon = { Icon(dest.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                selected = currentDestination == dest,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onNavigate(dest)
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .testTag("drawer_item_${dest.name.lowercase()}")
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            onClick = onSignOut,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag("drawer_sign_out")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign Out ($userEmail)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        CloukeTopBar(
                            currentDestination = currentDestination,
                            userEmail = userEmail,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onSignOut = onSignOut
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("mobile_bottom_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            val bottomDestinations = listOf(
                                AppDestination.CHAT,
                                AppDestination.COLTEF,
                                AppDestination.LIBRARY,
                                AppDestination.PROJECTS
                            )
                            for (dest in bottomDestinations) {
                                NavigationBarItem(
                                    selected = currentDestination == dest,
                                    onClick = { onNavigate(dest) },
                                    icon = { Icon(dest.icon, contentDescription = dest.title) },
                                    label = { Text(dest.shortLabel, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${dest.name.lowercase()}")
                                )
                            }

                            // "More" icon to open drawer
                            NavigationBarItem(
                                selected = false,
                                onClick = { scope.launch { drawerState.open() } },
                                icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "All Destinations") },
                                label = { Text("More", fontSize = 10.sp) },
                                modifier = Modifier.testTag("bottom_nav_more")
                            )
                        }
                    }
                ) { innerPadding ->
                    content(innerPadding)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloukeTopBar(
    currentDestination: AppDestination,
    userEmail: String,
    onMenuClick: (() -> Unit)?,
    onSignOut: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentDestination.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick, modifier = Modifier.testTag("open_drawer_btn")) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Navigation Menu")
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("user_profile_avatar")) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userEmail.firstOrNull()?.uppercase() ?: "C",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Signed in as", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(userEmail, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        onClick = {}
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onSignOut()
                        },
                        modifier = Modifier.testTag("menu_sign_out_btn")
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
