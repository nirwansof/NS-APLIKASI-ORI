package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    DASHBOARD,
    OFFLINE_MENU,
    BACAAN_SHOLAT,
    SUNNAH_SEHARI_HARI,
    DOA_QIYAMUL_LAIL,
    PEMBARUAN_KONTEN,
    DETAIL_VIEW,
    DOA_SEARCH,
    FAVORITES,
    DOA_KU,
    DZIKIR_PAGI_SORE
}

class PrayersViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("agama_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    val repository = PrayersRepository(database.prayerDao())

    // Backstack-based custom navigation
    private val _navigationStack = MutableStateFlow(listOf(Screen.DASHBOARD))
    val navigationStack: StateFlow<List<Screen>> = _navigationStack.asStateFlow()

    // Current selected prayer for Detail View
    private val _selectedPrayer = MutableStateFlow<PrayerItem?>(null)
    val selectedPrayer: StateFlow<PrayerItem?> = _selectedPrayer.asStateFlow()

    // UI state flows from Room
    val allPrayersList: StateFlow<List<PrayerItem>> = repository.getAllPrayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePrayersList: StateFlow<List<PrayerItem>> = repository.getFavoritePrayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doaList: StateFlow<List<PrayerItem>> = repository.getPrayersByCategory("doa")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sholatList: StateFlow<List<PrayerItem>> = repository.getPrayersByCategory("sholat")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sunnahList: StateFlow<List<PrayerItem>> = repository.getPrayersByCategory("sunnah")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val qiyamulList: StateFlow<List<PrayerItem>> = repository.getPrayersByCategory("qiyamul")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dzikirList: StateFlow<List<PrayerItem>> = repository.getPrayersByCategory("dzikir")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncQueueList: StateFlow<List<SyncQueueItem>> = repository.syncQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filter States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("Semua") // "Semua", "doa", "sholat", "sunnah", "qiyamul", "favorit"
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedSubCategoryFilter = MutableStateFlow("Semua")
    val selectedSubCategoryFilter: StateFlow<String> = _selectedSubCategoryFilter.asStateFlow()

    // Combined filtered list flow for search and category filtering
    val filteredPrayersList: StateFlow<List<PrayerItem>> = combine(
        allPrayersList,
        favoritePrayersList,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedSubCategoryFilter
    ) { all, favorites, query, categoryFilter, subCategoryFilter ->
        val baseList = when (categoryFilter) {
            "favorit" -> favorites
            "doa" -> all.filter { it.category == "doa" }
            "sholat" -> all.filter { it.category == "sholat" }
            "sunnah" -> all.filter { it.category == "sunnah" }
            "qiyamul" -> all.filter { it.category == "qiyamul" }
            else -> all
        }

        val subFiltered = if (subCategoryFilter == "Semua") {
            baseList
        } else {
            baseList.filter { it.subCategory.equals(subCategoryFilter, ignoreCase = true) }
        }

        if (query.isBlank()) {
            subFiltered
        } else {
            val q = query.trim().lowercase()
            subFiltered.filter { item ->
                item.title.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                item.subCategory.lowercase().contains(q) ||
                item.ayatListJson.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App configurations persisted in SharedPreferences
    private val _arabicFontSize = MutableStateFlow(sharedPrefs.getFloat("arabic_font_size", 26f))
    val arabicFontSize: StateFlow<Float> = _arabicFontSize.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isParagraphMode = MutableStateFlow(sharedPrefs.getBoolean("is_paragraph_mode", false))
    val isParagraphMode: StateFlow<Boolean> = _isParagraphMode.asStateFlow()

    fun navigateTo(screen: Screen, prayerItem: PrayerItem? = null) {
        if (prayerItem != null) {
            _selectedPrayer.value = prayerItem
        }
        _navigationStack.value = _navigationStack.value + screen
    }

    fun navigateBack() {
        val current = _navigationStack.value
        if (current.size > 1) {
            _navigationStack.value = current.dropLast(1)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setSubCategoryFilter(subCategory: String) {
        _selectedSubCategoryFilter.value = subCategory
    }

    fun toggleFavorite(prayerItem: PrayerItem) {
        viewModelScope.launch {
            val newFavStatus = !prayerItem.isFavorite
            repository.toggleFavorite(prayerItem.id, newFavStatus)
            if (_selectedPrayer.value?.id == prayerItem.id) {
                _selectedPrayer.value = prayerItem.copy(isFavorite = newFavStatus)
            }
        }
    }

    fun setArabicFontSize(size: Float) {
        _arabicFontSize.value = size
        sharedPrefs.edit().putFloat("arabic_font_size", size).apply()
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        sharedPrefs.edit().putBoolean("is_dark_theme", enabled).apply()
    }

    fun setParagraphMode(enabled: Boolean) {
        _isParagraphMode.value = enabled
        sharedPrefs.edit().putBoolean("is_paragraph_mode", enabled).apply()
    }

    fun runSync(onSuccess: (Int) -> Unit, onNoData: () -> Unit) {
        viewModelScope.launch {
            val queue = syncQueueList.value
            if (queue.isEmpty()) {
                onNoData()
            } else {
                val count = queue.size
                repository.syncQueueToLocal()
                onSuccess(count)
            }
        }
    }

    fun deletePrayerItem(id: Int) {
        viewModelScope.launch {
            repository.deletePrayer(id)
        }
    }
}
