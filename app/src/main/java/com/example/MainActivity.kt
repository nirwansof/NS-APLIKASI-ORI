package com.example

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Ayat
import com.example.data.PrayerItem
import com.example.data.SyncQueueItem
import com.example.ui.PrayersViewModel
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PrayersViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationContainer(viewModel)
                }
            }
        }
    }
}

private fun getScreenDepth(screen: Screen): Int {
    return when (screen) {
        Screen.DASHBOARD -> 0
        Screen.OFFLINE_MENU -> 1
        Screen.DOA_SEARCH -> 1
        Screen.FAVORITES -> 1
        Screen.DOA_KU -> 2
        Screen.DZIKIR_PAGI_SORE -> 2
        Screen.BACAAN_SHOLAT -> 2
        Screen.SUNNAH_SEHARI_HARI -> 2
        Screen.DOA_QIYAMUL_LAIL -> 2
        Screen.PEMBARUAN_KONTEN -> 2
        Screen.DETAIL_VIEW -> 3
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigationContainer(viewModel: PrayersViewModel) {
    val navigationStack by viewModel.navigationStack.collectAsState()
    val currentScreen = navigationStack.last()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val initialDepth = getScreenDepth(initialState)
                    val targetDepth = getScreenDepth(targetState)
                    val isForward = targetDepth >= initialDepth

                    val slideIn = slideInHorizontally(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        initialOffsetX = { width -> if (isForward) width else -width }
                    ) + fadeIn(animationSpec = tween(350))

                    val slideOut = slideOutHorizontally(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        targetOffsetX = { width -> if (isForward) -width else width }
                    ) + fadeOut(animationSpec = tween(350))

                    slideIn togetherWith slideOut
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.DASHBOARD -> DashboardScreen(viewModel)
                    Screen.OFFLINE_MENU -> OfflineMenuScreen(viewModel)
                    Screen.DOA_SEARCH -> DoaSearchScreen(viewModel)
                    Screen.FAVORITES -> FavoritesScreen(viewModel)
                    Screen.DOA_KU -> DoaKuScreen(viewModel)
                    Screen.DZIKIR_PAGI_SORE -> DzikirPagiSoreScreen(viewModel)
                    Screen.BACAAN_SHOLAT -> BacaanSholatScreen(viewModel)
                    Screen.SUNNAH_SEHARI_HARI -> SunnahSehariHariScreen(viewModel)
                    Screen.DOA_QIYAMUL_LAIL -> DoaQiyamulLailScreen(viewModel)
                    Screen.PEMBARUAN_KONTEN -> PembaruanKontenScreen(viewModel)
                    Screen.DETAIL_VIEW -> DetailViewScreen(viewModel)
                }
            }
        }
    }
}

// 1. DASHBOARD SCREEN
@Composable
fun DashboardScreen(viewModel: PrayersViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isParagraphMode by viewModel.isParagraphMode.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. Dashboard Header Card (integrating Title, Theme Toggle, View Mode Toggle, Font Size slider and Preview Box)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Row: Title and Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NS PRAY APP",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Format Per Ayat & Qiyamul Lail",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Theme switch label and switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { viewModel.setDarkTheme(it) },
                                modifier = Modifier.testTag("theme_toggle_switch")
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Row: View Mode Toggle & Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode Tampilan Switch (Per Ayat / Paragraf)
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = "Mode Tampilan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Per Ayat",
                                    fontSize = 11.sp,
                                    fontWeight = if (!isParagraphMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isParagraphMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Switch(
                                    checked = isParagraphMode,
                                    onCheckedChange = { viewModel.setParagraphMode(it) },
                                    modifier = Modifier.testTag("view_mode_toggle_switch")
                                )
                                Text(
                                    text = "Paragraf",
                                    fontSize = 11.sp,
                                    fontWeight = if (isParagraphMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isParagraphMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Ukuran Huruf Slider
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text(
                                text = "Ukuran Arab: ${arabicFontSize.toInt()} sp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = arabicFontSize,
                                onValueChange = { viewModel.setArabicFontSize(it) },
                                valueRange = 20f..40f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("font_slider")
                            )
                        }
                    }

                    // Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            fontSize = arabicFontSize.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 2. Hero Image Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Islamic Banner Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Overlay Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )
                    Text(
                        text = "Mendekat kepada Pencipta melalui ketulusan malam",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }

        // 3. Search & Favorites Quick Actions
        item {
            DashboardMenuItem(
                title = "Pencarian Doa & Harian",
                subtitle = "Cari doa harian dengan filter kategori dan kata kunci",
                icon = Icons.Default.Search,
                tag = "menu_search_doa_card",
                onClick = { viewModel.navigateTo(Screen.DOA_SEARCH) }
            )
        }

        item {
            DashboardMenuItem(
                title = "Doa Favorit Saya",
                subtitle = "Daftar doa dan dzikir pilihan yang telah disimpan",
                icon = Icons.Default.Favorite,
                tag = "menu_favorites_card",
                onClick = { viewModel.navigateTo(Screen.FAVORITES) }
            )
        }

        // 4. NS (offline) item
        item {
            DashboardMenuItem(
                title = "NS (offline)",
                subtitle = "Akses bacaan shalat, dzikir, doa, dan sunnah harian",
                icon = Icons.Default.Book,
                tag = "menu_offline_card",
                onClick = { viewModel.navigateTo(Screen.OFFLINE_MENU) }
            )
        }
    }
}

@Composable
fun DashboardMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// 2. OFFLINE MENU SCREEN
@Composable
fun OfflineMenuScreen(viewModel: PrayersViewModel) {
    val doaList by viewModel.doaList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "NS (offline)",
            onBack = { viewModel.navigateBack() }
        )

        Spacer(modifier = Modifier.height(4.dp))

        SubMenuItem(
            title = "Bacaan Shalat",
            subtitle = "Panduan bacaan shalat wajib dari Iftitah hingga selesai",
            onClick = { viewModel.navigateTo(Screen.BACAAN_SHOLAT) }
        )

        SubMenuItem(
            title = "Doa Qiyamul Lail",
            subtitle = "Panduan dzikir & doa shalat tahajud / witir di malam hari",
            onClick = { viewModel.navigateTo(Screen.DOA_QIYAMUL_LAIL) }
        )

        SubMenuItem(
            title = "DOAKU",
            subtitle = "Kumpulan doa harian, keselamatan & perlindungan",
            highlight = true,
            onClick = { viewModel.navigateTo(Screen.DOA_KU) }
        )

        SubMenuItem(
            title = "DZIKIR PAGI & SORE",
            subtitle = "Dzikir pagi & sore penentram hati & pelindung diri",
            highlight = true,
            onClick = { viewModel.navigateTo(Screen.DZIKIR_PAGI_SORE) }
        )

        SubMenuItem(
            title = "SUNNAHKU",
            subtitle = "Kumpulan adab dan amalan sunnah sehari-hari Rasulullah",
            onClick = { viewModel.navigateTo(Screen.SUNNAH_SEHARI_HARI) }
        )
    }
}

@Composable
fun SubMenuItem(
    title: String,
    subtitle: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = if (highlight) 1.2.dp else 1.dp,
            color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// 3. BACAAN SHOLAT SCREEN (LANGSUNG ISI SEMUA)
@Composable
fun BacaanSholatScreen(viewModel: PrayersViewModel) {
    val sholatList by viewModel.sholatList.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val isParagraphMode by viewModel.isParagraphMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Bacaan Shalat Lengkap",
            onBack = { viewModel.navigateBack() }
        )

        if (sholatList.isEmpty()) {
            EmptyStateIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sholatList) { item ->
                    AyatBlock(
                        title = item.title,
                        ayatList = viewModel.repository.fromJson(item.ayatListJson),
                        arabicFontSize = arabicFontSize,
                        isCustom = item.isCustom,
                        isParagraphMode = isParagraphMode,
                        onDeleteCustom = { viewModel.deletePrayerItem(item.id) }
                    )
                }
            }
        }
    }
}

// 4. SUNNAH SEHARI-HARI SCREEN (LIST)
@Composable
fun SunnahSehariHariScreen(viewModel: PrayersViewModel) {
    val sunnahList by viewModel.sunnahList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Sunnah Sehari-hari",
            onBack = { viewModel.navigateBack() }
        )

        if (sunnahList.isEmpty()) {
            EmptyStateIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sunnahList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.DETAIL_VIEW, item) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.isCustom) {
                                        Text(
                                            text = "Ditambahkan oleh Admin",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.isCustom) {
                                    IconButton(
                                        onClick = { viewModel.deletePrayerItem(item.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Konten Kustom",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. DETAIL VIEW (UNIVERSAL)
@Composable
fun DetailViewScreen(viewModel: PrayersViewModel) {
    val selectedPrayer by viewModel.selectedPrayer.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val isParagraphMode by viewModel.isParagraphMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = selectedPrayer?.title ?: "Detail Bacaan",
            onBack = { viewModel.navigateBack() }
        )

        selectedPrayer?.let { prayer ->
            val ayatList = viewModel.repository.fromJson(prayer.ayatListJson)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    AyatBlock(
                        title = prayer.title,
                        ayatList = ayatList,
                        arabicFontSize = arabicFontSize,
                        isCustom = prayer.isCustom,
                        isParagraphMode = isParagraphMode,
                        isFavorite = prayer.isFavorite,
                        onToggleFavorite = { viewModel.toggleFavorite(prayer) },
                        onDeleteCustom = {
                            viewModel.deletePrayerItem(prayer.id)
                            viewModel.navigateBack()
                        }
                    )
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada detail yang dipilih.")
        }
    }
}

// 6. QIYAMUL LAIL SCREEN (LANGSUNG ISI SEMUA)
@Composable
fun DoaQiyamulLailScreen(viewModel: PrayersViewModel) {
    val qiyamulList by viewModel.qiyamulList.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val isParagraphMode by viewModel.isParagraphMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Doa Qiyamul Lail",
            onBack = { viewModel.navigateBack() }
        )

        if (qiyamulList.isEmpty()) {
            EmptyStateIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(qiyamulList) { item ->
                    AyatBlock(
                        title = item.title,
                        ayatList = viewModel.repository.fromJson(item.ayatListJson),
                        arabicFontSize = arabicFontSize,
                        isCustom = item.isCustom,
                        isParagraphMode = isParagraphMode,
                        onDeleteCustom = { viewModel.deletePrayerItem(item.id) }
                    )
                }
            }
        }
    }
}

// 7. PEMBARUAN KONTEN SCREEN
@Composable
fun PembaruanKontenScreen(viewModel: PrayersViewModel) {
    val syncQueueList by viewModel.syncQueueList.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(
                title = "Pembaruan Konten",
                onBack = { viewModel.navigateBack() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sinkronisasi Offline",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Tekan tombol di bawah untuk menyinkronkan data teks baru hasil ekstraksi gambar dari panel admin ke dalam penyimpanan luring (offline) HP Anda.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pending Queue Indicator card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${syncQueueList.size}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Konten Baru Siap Disinkronkan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.runSync(
                    onSuccess = { count ->
                        Toast.makeText(
                            context,
                            "Berhasil sinkron! $count item konten baru berhasil disuntikkan ke sub-menu offline HP.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onNoData = {
                        Toast.makeText(context, "Data luring Anda sudah sinkron.", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("execute_sync_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("JALANKAN SINKRONISASI", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 8. SEARCH & CATEGORY FILTERING SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoaSearchScreen(viewModel: PrayersViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedSubCategory by viewModel.selectedSubCategoryFilter.collectAsState()
    val filteredList by viewModel.filteredPrayersList.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()

    val categories = listOf(
        "Semua" to "Semua",
        "Doa Harian" to "doa",
        "Bacaan Sholat" to "sholat",
        "Sunnah Sehari-hari" to "sunnah",
        "Qiyamul Lail" to "qiyamul",
        "Favorit Saya" to "favorit"
    )

    val subCategories = listOf(
        "Semua", "Kesulitan", "Pagi & Petang", "Perlindungan", "Permohonan", "Keluarga", "Ibadah", "Pakaian & Makan"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Pencarian Doa & Harian",
            onBack = { viewModel.navigateBack() }
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("doa_search_text_field"),
            placeholder = { Text("Cari judul, latin, atau terjemahan...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Bersihkan pencarian"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )

        // Main Category Filter Chips
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Kategori Utama",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(categories) { (label, value) ->
                    val isSelected = selectedCategory == value
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(value) },
                        label = { Text(label, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Subcategory Filter Chips
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Filter Topik / Sub-Kategori",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(subCategories) { sub ->
                    val isSelected = selectedSubCategory == sub
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSubCategoryFilter(sub) },
                        label = { Text(sub, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ditemukan: ${filteredList.size} doa",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (searchQuery.isNotEmpty() || selectedCategory != "Semua" || selectedSubCategory != "Semua") {
                TextButton(
                    onClick = {
                        viewModel.setSearchQuery("")
                        viewModel.setCategoryFilter("Semua")
                        viewModel.setSubCategoryFilter("Semua")
                    }
                ) {
                    Text("Reset Filter", fontSize = 12.sp)
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Tidak ditemukan doa yang sesuai pencarian.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    PrayerCardItem(
                        item = item,
                        arabicFontSize = arabicFontSize,
                        onFavoriteClick = { viewModel.toggleFavorite(item) },
                        onClick = { viewModel.navigateTo(Screen.DETAIL_VIEW, item) }
                    )
                }
            }
        }
    }
}

// 9. FAVORITES SCREEN
@Composable
fun FavoritesScreen(viewModel: PrayersViewModel) {
    val favoriteList by viewModel.favoritePrayersList.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Doa Favorit Saya",
            onBack = { viewModel.navigateBack() }
        )

        if (favoriteList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Belum Ada Doa Favorit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Klik ikon hati pada daftar doa mana saja untuk menyimpannya sebagai favorit di sini.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.navigateTo(Screen.DOA_SEARCH) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cari & Jelajahi Doa")
                    }
                }
            }
        } else {
            Text(
                text = "Tersimpan: ${favoriteList.size} doa favorit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(favoriteList, key = { it.id }) { item ->
                    PrayerCardItem(
                        item = item,
                        arabicFontSize = arabicFontSize,
                        onFavoriteClick = { viewModel.toggleFavorite(item) },
                        onClick = { viewModel.navigateTo(Screen.DETAIL_VIEW, item) }
                    )
                }
            }
        }
    }
}

// 10. DOAKU SCREEN
@Composable
fun DoaKuScreen(viewModel: PrayersViewModel) {
    val doaList by viewModel.doaList.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "DOAKU - Doa Harian",
            onBack = { viewModel.navigateBack() }
        )

        Text(
            text = "Kumpulan ${doaList.size} doa harian pilihan untuk keberkahan & perlindungan",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        if (doaList.isEmpty()) {
            EmptyStateIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(doaList, key = { it.id }) { item ->
                    PrayerCardItem(
                        item = item,
                        arabicFontSize = arabicFontSize,
                        onFavoriteClick = { viewModel.toggleFavorite(item) },
                        onClick = { viewModel.navigateTo(Screen.DETAIL_VIEW, item) }
                    )
                }
            }
        }
    }
}

// 11. DZIKIR PAGI & SORE SCREEN
@Composable
fun DzikirPagiSoreScreen(viewModel: PrayersViewModel) {
    val dzikirList by viewModel.dzikirList.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "DZIKIR PAGI & SORE",
            onBack = { viewModel.navigateBack() }
        )

        Text(
            text = "Kumpulan ${dzikirList.size} amalan dzikir pagi dan sore sesuai sunnah",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        if (dzikirList.isEmpty()) {
            EmptyStateIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(dzikirList, key = { it.id }) { item ->
                    PrayerCardItem(
                        item = item,
                        arabicFontSize = arabicFontSize,
                        onFavoriteClick = { viewModel.toggleFavorite(item) },
                        onClick = { viewModel.navigateTo(Screen.DETAIL_VIEW, item) }
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerCardItem(
    item: PrayerItem,
    arabicFontSize: Float,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("prayer_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.subCategory.isNotBlank()) {
                        Text(
                            text = item.subCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.testTag("fav_button_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Preview category tag
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = when(item.category) {
                        "doa" -> "Doa Harian"
                        "sholat" -> "Bacaan Shalat"
                        "sunnah" -> "Sunnah"
                        "qiyamul" -> "Qiyamul Lail"
                        else -> item.category.uppercase()
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// --- SHARED REUSABLE COMPONENTS ---

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun EmptyStateIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Belum ada data. Silakan sync atau tambah dari admin.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AyatBlock(
    title: String,
    ayatList: List<Ayat>,
    arabicFontSize: Float,
    isCustom: Boolean,
    isParagraphMode: Boolean,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onDeleteCustom: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header of Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleFavorite != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("detail_fav_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorit",
                                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isCustom) {
                        IconButton(
                            onClick = onDeleteCustom,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            if (isParagraphMode) {
                // Render as single combined paragraph for Arab, Latin, and Terjemahan
                val joinedArab = ayatList.mapIndexed { index, ayat ->
                    val numberOnly = ayat.no.replace(Regex("\\D+"), "")
                    val numStr = if (numberOnly.isNotEmpty()) numberOnly else (index + 1).toString()
                    "${ayat.arab} ﴿$numStr﴾"
                }.joinToString(separator = " ")

                val joinedLatin = ayatList.mapIndexed { index, ayat ->
                    val numberOnly = ayat.no.replace(Regex("\\D+"), "")
                    val numStr = if (numberOnly.isNotEmpty()) numberOnly else (index + 1).toString()
                    "${ayat.latin} ($numStr) •"
                }.joinToString(separator = " ")

                val joinedTerjemahan = ayatList.mapIndexed { index, ayat ->
                    val numberOnly = ayat.no.replace(Regex("\\D+"), "")
                    val numStr = if (numberOnly.isNotEmpty()) numberOnly else (index + 1).toString()
                    "${ayat.terjemahan} ($numStr) •"
                }.joinToString(separator = " ")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Arabic Paragraph
                    Text(
                        text = joinedArab,
                        fontSize = arabicFontSize.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        lineHeight = (arabicFontSize * 1.8).sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Latin Paragraph
                    Text(
                        text = joinedLatin,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Translation Paragraph
                    Text(
                        text = joinedTerjemahan,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Verses List (Default Per Ayat Mode)
                ayatList.forEach { ayat ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Verse Number
                        Text(
                            text = ayat.no,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Arabic Text
                        Text(
                            text = ayat.arab,
                            fontSize = arabicFontSize.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right,
                            lineHeight = (arabicFontSize * 1.8).sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Latin Text
                        Text(
                            text = ayat.latin,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Translation Text
                        Text(
                            text = ayat.terjemahan,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}


