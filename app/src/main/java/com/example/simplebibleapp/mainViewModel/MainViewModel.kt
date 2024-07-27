package com.example.simplebibleapp.mainViewModel

import androidx.lifecycle.ViewModel
import com.example.simplebibleapp.DEFAULT_FONT_SIZE
import com.example.simplebibleapp.dataClasses.DisplayConfig
import com.example.simplebibleapp.dataClasses.LOWERLIMIT
import com.example.simplebibleapp.dataClasses.Selection
import com.example.simplebibleapp.dataClasses.UPPERLIMIT
import com.example.simplebibleapp.dataClasses.sChangeBookIndex
import com.example.simplebibleapp.dataClasses.sChangeChapter
import com.example.simplebibleapp.dataClasses.sChangeTranslation
import com.example.simplebibleapp.readBibleData.DEFAULT_TRANSLATION
import com.example.simplebibleapp.repositories.DataStoreRepository
import com.example.simplebibleapp.repositories.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class MainViewModel(val dataStoreRepository: DataStoreRepository, val historyRepository: HistoryRepository) : ViewModel() {
    lateinit var uiState: StateFlow<MainUiState>;
    private lateinit var _uiState : MutableStateFlow<MainUiState>;

    /*
    ChapterSelection functions
     */

    fun setTranslation(translationName: String) {
        _setTranslation(translationName)
        runBlocking {
            launch {
                dataStoreRepository.saveTranslationToDataStore(translationName)
            }
        }
    }
    private fun _setTranslation(translationName: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selection = sChangeTranslation(currentState.selection, translationName)
            )
        }
    }

    fun setBookIndex(bookIndex: Int) {
        _setBookIndex(bookIndex)
        setChapter(1) // because all books have at least chapter 1
        runBlocking {
            launch {
                dataStoreRepository.saveBookIndexToDataStore(bookIndex)
            }
        }
    }
    private fun _setBookIndex(bookIndex: Int) {
        _uiState.update { currentState -> currentState.copy(
            selection = sChangeBookIndex(currentState.selection, bookIndex)
        )}
    }

    fun setChapter(chapter: Int) {
        _setChapter(chapter)
        runBlocking {
            launch {
                dataStoreRepository.saveChapterToDataStore(chapter)
            }
        }
    }
    private fun _setChapter(chapter: Int) {
        _uiState.update { currentState -> currentState.copy(
            selection = sChangeChapter(currentState.selection, chapter)
        )}
    }

    /*
      Zoom functions
     */
    fun onZoom(gestureZoom: Float) {
        val newScale = _uiState.value.displayConfig.zoom * gestureZoom
        // clamp zoom
        if (newScale > LOWERLIMIT && newScale < UPPERLIMIT) {
            setZoom(newScale)
        }
    }

    private fun setZoom(zoom: Float) {
        _setZoom(zoom)
        runBlocking {
            launch {
                dataStoreRepository.saveZoomToDataStore(zoom)
            }
        }
    }

    private fun _setZoom(zoom: Float) {
        _uiState.update { currentState -> currentState.copy(
            displayConfig = DisplayConfig(DEFAULT_FONT_SIZE.value, zoom)
        )}
    }

    /*
    History functions
     */
    suspend fun insertSelection(selection: Selection) {
        historyRepository.insert(selection)
    }
    fun getAllHistory() : Flow<List<Selection>> {
        return historyRepository.getAll.map { list ->
            list.map { historyRecord ->
                historyRecord.selection
            }
        }
    }
    /*
    ViewModel functions
     */
    fun resetApp() {
        _uiState.value = newMainUiState()
    }

    fun newMainUiState() : MainUiState {
        return MainUiState(
            selection = Selection(chapter = 1, bookIndex = 0, translation = DEFAULT_TRANSLATION),
            displayConfig = DisplayConfig(DEFAULT_FONT_SIZE.value, 1f)
        )
    }
    init {
        runBlocking {
            launch {
                // must initialise _uiState first
                _uiState = MutableStateFlow(newMainUiState())
                uiState = _uiState.asStateFlow()

                resetApp()

                val dsr = dataStoreRepository
                _setBookIndex(dsr.getBookIndexFromDataStore())
                _setChapter(dsr.getChapterFromDataStore())
                _setZoom(dsr.getZoomFromDataStore())
                _setTranslation(dsr.getTranslationFromDataStore())
            }
        }
    }

}
