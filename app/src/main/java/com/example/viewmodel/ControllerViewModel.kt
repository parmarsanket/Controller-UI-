package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.ControllerDatabase
import com.example.db.LayoutEntity
import com.example.model.ControllerInput
import com.example.model.InputType
import com.example.model.LayoutPresets
import com.example.repository.LayoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ControllerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ControllerDatabase.getDatabase(application)
    private val repository = LayoutRepository(db.layoutDao())

    // All layout profiles stored in database reactive list
    val layouts: StateFlow<List<LayoutEntity>> = repository.allLayouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selection state definitions
    private val _selectedLayoutId = MutableStateFlow("preset_standard")
    val selectedLayoutId: StateFlow<String> = _selectedLayoutId.asStateFlow()

    // Current active inputs placement representation
    private val _activeInputs = MutableStateFlow<List<ControllerInput>>(emptyList())
    val activeInputs: StateFlow<List<ControllerInput>> = _activeInputs.asStateFlow()

    // Editor vs Play mode selector
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Landscape Immersive Play Mode Active
    private val _playModeActive = MutableStateFlow(false)
    val playModeActive: StateFlow<Boolean> = _playModeActive.asStateFlow()

    // Selected input in HUD editor for tuning sliders/textfields
    private val _selectedInputId = MutableStateFlow<String?>(null)
    val selectedInputId: StateFlow<String?> = _selectedInputId.asStateFlow()

    // Toggle switch for RBG backlight glow
    private val _rgbEnabled = MutableStateFlow(true)
    val rgbEnabled: StateFlow<Boolean> = _rgbEnabled.asStateFlow()

    // Play Mode real-time physics interactions states
    val joystickOffsets = mutableStateMapOf<String, Offset>() // id -> Offset(-1f..1f, -1f..1f)
    val triggerDepths = mutableStateMapOf<String, Float>() // id -> depth (0f..1f)
    val buttonPressed = mutableStateMapOf<String, Boolean>() // id -> pressed
    val dpadActiveDirection = mutableStateMapOf<String, String?>() // id -> "UP", "DOWN", "LEFT", "RIGHT"

    init {
        // Automatically sync inputs when the current layout selection changes
        viewModelScope.launch {
            combine(layouts, _selectedLayoutId) { layoutList, selectedId ->
                val entity = layoutList.find { it.id == selectedId }
                if (entity != null) {
                    repository.deserializeInputs(entity.inputsJson)
                } else if (_activeInputs.value.isEmpty()) {
                    LayoutPresets.getStandardLayout()
                } else {
                    null
                }
            }.collect { resolvedInputs ->
                if (resolvedInputs != null) {
                    _activeInputs.value = resolvedInputs
                    // Reset interaction states
                    joystickOffsets.clear()
                    triggerDepths.clear()
                    buttonPressed.clear()
                    dpadActiveDirection.clear()
                }
            }
        }
    }

    // Toggle lighting
    fun toggleRGB() {
        _rgbEnabled.value = !_rgbEnabled.value
    }

    // Toggle play vs editor Mode
    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        _selectedInputId.value = null
    }

    fun setPlayModeActive(active: Boolean) {
        _playModeActive.value = active
        if (active) {
            _isEditMode.value = false
            _selectedInputId.value = null
        }
    }

    // Selection layout
    fun selectLayout(id: String) {
        _selectedLayoutId.value = id
        _selectedInputId.value = null
    }

    fun selectInputToEdit(id: String?) {
        if (_isEditMode.value) {
            _selectedInputId.value = id
        }
    }

    // Drag-and-drop coordinate position updates (percentage coordinates)
    fun updateInputPosition(id: String, pctX: Float, pctY: Float) {
        _activeInputs.value = _activeInputs.value.map { input ->
            if (input.id == id) {
                // Keep clamps safe within visual limits of standard 16:9 container card
                input.copy(
                    pctX = pctX.coerceIn(0.01f, 0.99f),
                    pctY = pctY.coerceIn(0.01f, 0.99f)
                )
            } else input
        }
    }

    // Customize action mapped label name
    fun updateActionName(id: String, newName: String) {
        _activeInputs.value = _activeInputs.value.map { input ->
            if (input.id == id) {
                input.copy(actionName = newName)
            } else input
        }
    }

    // Customize item visual scale factor
    fun updateInputScale(id: String, newScale: Float) {
        _activeInputs.value = _activeInputs.value.map { input ->
            if (input.id == id) {
                input.copy(scale = newScale.coerceIn(0.5f, 2.0f))
            } else input
        }
    }

    // Remove key from screen
    fun removeInput(id: String) {
        if (_selectedInputId.value == id) {
            _selectedInputId.value = null
        }
        _activeInputs.value = _activeInputs.value.filterNot { it.id == id }
    }

    // Obtain unused buttons of controller library
    fun getAvailableUnplacedInputs(): List<InputType> {
        val placedTypes = _activeInputs.value.map { it.type }.toSet()
        return InputType.values().filter { it !in placedTypes }
    }

    // Add key to layout
    fun addInputFromLibrary(type: InputType) {
        val baseId = type.name
        
        // Prevent duplicates
        if (_activeInputs.value.any { it.id == baseId }) return

        // Position placed symmetrically in the center of cards
        val newInput = ControllerInput(
            id = baseId,
            type = type,
            pctX = 0.5f,
            pctY = 0.5f,
            actionName = "Custom Action",
            scale = 1.0f
        )
        _activeInputs.value = _activeInputs.value + newInput
        _selectedInputId.value = baseId // Select added view automatically to configure
    }

    // Create custom user layout profile
    fun createNewCustomLayout(name: String) {
        viewModelScope.launch {
            val uuid = "custom_" + UUID.randomUUID().toString().take(6)
            // Starts with copy of standard layout inputs
            val currentInputs = _activeInputs.value.ifEmpty { LayoutPresets.getStandardLayout() }
            repository.saveCustomLayout(uuid, name, currentInputs)
            _selectedLayoutId.value = uuid
        }
    }

    // Push saving current updates back to persistent SQLite profile
    fun saveActiveLayoutChanges() {
        viewModelScope.launch {
            val currentId = _selectedLayoutId.value
            val currentList = layouts.value
            val activeLayoutEntity = currentList.find { it.id == currentId } ?: return@launch
            
            // Layout is standard preset, then force-create custom clone
            if (activeLayoutEntity.isPreset) {
                val clonedId = "custom_" + UUID.randomUUID().toString().take(6)
                repository.saveCustomLayout(
                    id = clonedId,
                    name = "Custom ${activeLayoutEntity.name} Clone",
                    inputs = _activeInputs.value
                )
                _selectedLayoutId.value = clonedId
            } else {
                repository.saveCustomLayout(
                    id = currentId,
                    name = activeLayoutEntity.name,
                    inputs = _activeInputs.value
                )
            }
        }
    }

    // Delete custom layouts profile
    fun deleteActiveLayoutProfile() {
        val currentId = _selectedLayoutId.value
        viewModelScope.launch {
            repository.deleteCustomLayout(currentId)
            // Rollback to classic standard preset layout
            _selectedLayoutId.value = "preset_standard"
        }
    }

    // Rename layout
    fun renameActiveLayoutProfile(newName: String) {
        val currentId = _selectedLayoutId.value
        viewModelScope.launch {
            val activeLayoutEntity = layouts.value.find { it.id == currentId } ?: return@launch
            if (!activeLayoutEntity.isPreset) {
                repository.saveCustomLayout(
                    id = currentId,
                    name = newName,
                    inputs = _activeInputs.value
                )
            }
        }
    }

    // Play interaction events
    fun handlePress(id: String, pressed: Boolean) {
        buttonPressed[id] = pressed
    }

    fun handleJoystickMove(id: String, offset: Offset) {
        joystickOffsets[id] = offset
    }

    fun handleTriggerDepth(id: String, depth: Float) {
        triggerDepths[id] = depth
    }

    fun handleDPadPress(id: String, direction: String?) {
        dpadActiveDirection[id] = direction
    }
}
