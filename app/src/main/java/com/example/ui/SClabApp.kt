package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.FounderRepository
import com.example.data.repository.MembershipRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.ui.screens.ActiveAudioCallScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.BankDetailsScreen
import com.example.ui.screens.CallRootContainer
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ConversationListScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.LegalPoliciesViewerScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MembershipSelectionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.PrimaryVioletLight
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CallUiState
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.MembershipViewModel
import com.example.ui.viewmodel.WalletViewModel

enum class MainTab {
    DISCOVER,
    CHATS,
    WALLET,
    PROFILE
}

enum class AppScreen {
    SPLASH,
    LOGIN,
    REGISTER,
    EMAIL_VERIFICATION,
    MEMBERSHIP,
    MAIN_TABS,
    CHAT_DETAIL,
    BANK_DETAILS,
    TRANSACTION_HISTORY,
    LEGAL_POLICY,
    ADMIN_LOGIN,
    ADMIN_DASHBOARD
}

@Composable
fun SClabApp() {
    val context = LocalContext.current

    // Repositories
    val authRepo = remember { AuthRepository(context) }
    val userRepo = remember { UserRepository(context) }
    val founderRepo = remember { FounderRepository(context) }
    val membershipRepo = remember { MembershipRepository(context) }
    val chatRepo = remember { ChatRepository(context) }
    val walletRepo = remember { WalletRepository(context) }
    val callRepo = remember { CallRepository(context, walletRepo) }
    val adminRepo = remember { AdminRepository(context) }

    // Root ViewModels
    val authViewModel = remember { AuthViewModel(authRepo, userRepo, founderRepo) }
    val homeViewModel = remember { HomeViewModel(userRepo, membershipRepo) }
    val adminViewModel = remember { AdminViewModel(adminRepo) }

    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var currentTab by remember { mutableStateOf(MainTab.DISCOVER) }

    // Active sub-screen parameters
    var activeChatOtherName by remember { mutableStateOf("") }
    var activeChatOtherId by remember { mutableStateOf("") }
    var activeChatConvId by remember { mutableStateOf("") }
    var legalTitle by remember { mutableStateOf("") }
    var legalContent by remember { mutableStateOf("") }

    val authState by authViewModel.uiState.collectAsState()
    val currentUser = (authState as? AuthUiState.Authenticated)?.user

    val chatViewModel = remember(currentUser?.uid) {
        ChatViewModel(chatRepo, currentUser?.uid)
    }

    // Scoped ViewModels
    val membershipViewModel = remember(currentUser?.uid) {
        MembershipViewModel(
            membershipRepository = membershipRepo,
            founderRepository = founderRepo,
            userId = currentUser?.uid ?: "",
            userEmail = currentUser?.email ?: ""
        )
    }

    val walletViewModel = remember(currentUser?.uid) {
        WalletViewModel(
            walletRepository = walletRepo,
            userId = currentUser?.uid ?: ""
        )
    }

    val callViewModel = remember(currentUser?.uid) {
        CallViewModel(
            callRepository = callRepo,
            currentUserId = currentUser?.uid ?: "",
            earningRatePaisePer15s = 400L
        )
    }

    // Call Overlay Host
    val callUiState by callViewModel.uiState.collectAsState()
    val isCallActive = callUiState !is CallUiState.Idle

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onTimeout = {
                        if (currentUser != null) {
                            currentScreen = AppScreen.MAIN_TABS
                        } else {
                            currentScreen = AppScreen.LOGIN
                        }
                    }
                )
            }

            AppScreen.LOGIN -> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { currentScreen = AppScreen.REGISTER },
                    onNavigateToAdminLogin = { currentScreen = AppScreen.ADMIN_LOGIN },
                    onLoginSuccess = {
                        currentScreen = AppScreen.MEMBERSHIP
                    }
                )
            }

            AppScreen.REGISTER -> {
                BackHandler { currentScreen = AppScreen.LOGIN }
                RegisterScreen(
                    authViewModel = authViewModel,
                    onRegistrationSuccess = { isFounder ->
                        currentScreen = AppScreen.MEMBERSHIP
                    },
                    onNavigateToLogin = { currentScreen = AppScreen.LOGIN }
                )
            }

            AppScreen.EMAIL_VERIFICATION -> {
                BackHandler { currentScreen = AppScreen.LOGIN }
                EmailVerificationScreen(
                    userEmail = currentUser?.email ?: "",
                    onContinue = { currentScreen = AppScreen.MEMBERSHIP }
                )
            }

            AppScreen.MEMBERSHIP -> {
                BackHandler { currentScreen = AppScreen.LOGIN }
                MembershipSelectionScreen(
                    membershipViewModel = membershipViewModel,
                    onMembershipConfirmed = {
                        currentScreen = AppScreen.MAIN_TABS
                    }
                )
            }

            AppScreen.MAIN_TABS -> {
                Scaffold(
                    containerColor = DarkBackground,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            modifier = Modifier.navigationBarsPadding(),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.DISCOVER,
                                onClick = { currentTab = MainTab.DISCOVER },
                                icon = {
                                    Icon(
                                        if (currentTab == MainTab.DISCOVER) Icons.Filled.People else Icons.Outlined.People,
                                        contentDescription = "Discover"
                                    )
                                },
                                label = { Text("Discover") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = PrimaryViolet
                                ),
                                modifier = Modifier.testTag("tab_discover")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.CHATS,
                                onClick = { currentTab = MainTab.CHATS },
                                icon = {
                                    Icon(
                                        if (currentTab == MainTab.CHATS) Icons.Filled.Chat else Icons.Outlined.Chat,
                                        contentDescription = "Chats"
                                    )
                                },
                                label = { Text("Chats") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = PrimaryViolet
                                ),
                                modifier = Modifier.testTag("tab_chats")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.WALLET,
                                onClick = { currentTab = MainTab.WALLET },
                                icon = {
                                    Icon(
                                        if (currentTab == MainTab.WALLET) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = "Wallet"
                                    )
                                },
                                label = { Text("Wallet") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = PrimaryViolet
                                ),
                                modifier = Modifier.testTag("tab_wallet")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.PROFILE,
                                onClick = { currentTab = MainTab.PROFILE },
                                icon = {
                                    Icon(
                                        if (currentTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                },
                                label = { Text("Profile") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = PrimaryViolet
                                ),
                                modifier = Modifier.testTag("tab_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            MainTab.DISCOVER -> {
                                DiscoverScreen(
                                    homeViewModel = homeViewModel,
                                    onStartChat = { targetId, targetName ->
                                        chatViewModel.startConversationWith(
                                            targetUserId = targetId,
                                            targetUserName = targetName,
                                            currentUserName = currentUser?.displayName ?: "Me"
                                        ) { convId ->
                                            activeChatConvId = convId
                                            activeChatOtherName = targetName
                                            activeChatOtherId = targetId
                                            currentScreen = AppScreen.CHAT_DETAIL
                                        }
                                    },
                                    onStartAudioCall = { targetId, targetName ->
                                        callViewModel.startCall(
                                            receiverId = targetId,
                                            receiverName = targetName,
                                            callerName = currentUser?.displayName ?: "Caller",
                                            callType = "AUDIO"
                                        )
                                    },
                                    onStartVideoCall = { targetId, targetName ->
                                        callViewModel.startCall(
                                            receiverId = targetId,
                                            receiverName = targetName,
                                            callerName = currentUser?.displayName ?: "Caller",
                                            callType = "VIDEO"
                                        )
                                    },
                                    onOpenMembership = { currentScreen = AppScreen.MEMBERSHIP }
                                )
                            }

                            MainTab.CHATS -> {
                                ConversationListScreen(
                                    chatViewModel = chatViewModel,
                                    currentUserId = currentUser?.uid ?: "",
                                    onOpenConversation = { convId, otherName, otherId ->
                                        activeChatConvId = convId
                                        activeChatOtherName = otherName
                                        activeChatOtherId = otherId
                                        currentScreen = AppScreen.CHAT_DETAIL
                                    }
                                )
                            }

                            MainTab.WALLET -> {
                                WalletScreen(
                                    walletViewModel = walletViewModel,
                                    onOpenBankDetails = { currentScreen = AppScreen.BANK_DETAILS },
                                    onOpenTransactionHistory = { currentScreen = AppScreen.TRANSACTION_HISTORY }
                                )
                            }

                            MainTab.PROFILE -> {
                                ProfileScreen(
                                    homeViewModel = homeViewModel,
                                    authViewModel = authViewModel,
                                    onOpenLegalPolicy = { title, content ->
                                        legalTitle = title
                                        legalContent = content
                                        currentScreen = AppScreen.LEGAL_POLICY
                                    },
                                    onSignOut = { currentScreen = AppScreen.LOGIN }
                                )
                            }
                        }
                    }
                }
            }

            AppScreen.CHAT_DETAIL -> {
                BackHandler { currentScreen = AppScreen.MAIN_TABS }
                ChatDetailScreen(
                    chatViewModel = chatViewModel,
                    conversationId = activeChatConvId,
                    otherUserName = activeChatOtherName,
                    otherUserId = activeChatOtherId,
                    currentUserId = currentUser?.uid ?: "",
                    currentUserName = currentUser?.displayName ?: "Me",
                    onBack = { currentScreen = AppScreen.MAIN_TABS },
                    onAudioCall = {
                        callViewModel.startCall(
                            receiverId = activeChatOtherId,
                            receiverName = activeChatOtherName,
                            callerName = currentUser?.displayName ?: "Caller",
                            callType = "AUDIO"
                        )
                    },
                    onVideoCall = {
                        callViewModel.startCall(
                            receiverId = activeChatOtherId,
                            receiverName = activeChatOtherName,
                            callerName = currentUser?.displayName ?: "Caller",
                            callType = "VIDEO"
                        )
                    }
                )
            }

            AppScreen.BANK_DETAILS -> {
                BackHandler { currentScreen = AppScreen.MAIN_TABS }
                BankDetailsScreen(
                    walletViewModel = walletViewModel,
                    onBack = { currentScreen = AppScreen.MAIN_TABS }
                )
            }

            AppScreen.TRANSACTION_HISTORY -> {
                BackHandler { currentScreen = AppScreen.MAIN_TABS }
                TransactionHistoryScreen(
                    walletViewModel = walletViewModel,
                    onBack = { currentScreen = AppScreen.MAIN_TABS }
                )
            }

            AppScreen.LEGAL_POLICY -> {
                BackHandler { currentScreen = AppScreen.MAIN_TABS }
                LegalPoliciesViewerScreen(
                    title = legalTitle,
                    content = legalContent,
                    onBack = { currentScreen = AppScreen.MAIN_TABS }
                )
            }

            AppScreen.ADMIN_LOGIN -> {
                BackHandler { currentScreen = AppScreen.LOGIN }
                AdminLoginScreen(
                    adminViewModel = adminViewModel,
                    onLoginSuccess = { currentScreen = AppScreen.ADMIN_DASHBOARD },
                    onBack = { currentScreen = AppScreen.LOGIN }
                )
            }

            AppScreen.ADMIN_DASHBOARD -> {
                BackHandler { currentScreen = AppScreen.MAIN_TABS }
                AdminDashboardScreen(
                    adminViewModel = adminViewModel,
                    onExitAdmin = { currentScreen = AppScreen.MAIN_TABS }
                )
            }
        }

        // Overlay call container for active incoming/outgoing/connected calls
        if (isCallActive) {
            CallRootContainer(
                callViewModel = callViewModel,
                onCallFinished = { /* call ended */ }
            )
        }
    }
}
