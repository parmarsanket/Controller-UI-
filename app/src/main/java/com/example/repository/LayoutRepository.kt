package com.example.repository

import com.example.db.LayoutDao
import com.example.db.LayoutEntity
import com.example.model.ControllerInput
import com.example.model.LayoutPresets
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart

class LayoutRepository(private val layoutDao: LayoutDao) {

    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, ControllerInput::class.java)
    private val listAdapter = moshi.adapter<List<ControllerInput>>(listType)

    fun serializeInputs(inputs: List<ControllerInput>): String {
        return listAdapter.toJson(inputs)
    }

    fun deserializeInputs(json: String): List<ControllerInput> {
        return try {
            listAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // List of layouts starting with pre-populations
    val allLayouts: Flow<List<LayoutEntity>> = layoutDao.getAllLayouts()
        .onStart {
            ensurePresetsExist()
        }
        .conflate()

    private suspend fun ensurePresetsExist() {
        val standardPreset = layoutDao.getLayoutById("preset_standard")
        if (standardPreset == null) {
            layoutDao.insertLayout(
                LayoutEntity(
                    id = "preset_standard",
                    name = "Standard Xbox Layout",
                    isPreset = true,
                    inputsJson = serializeInputs(LayoutPresets.getStandardLayout())
                )
            )
        }

        val racingPreset = layoutDao.getLayoutById("preset_racing")
        if (racingPreset == null) {
            layoutDao.insertLayout(
                LayoutEntity(
                    id = "preset_racing",
                    name = "Pro Racing Layout",
                    isPreset = true,
                    inputsJson = serializeInputs(LayoutPresets.getRacingLayout())
                )
            )
        }

        val simPreset = layoutDao.getLayoutById("preset_sim")
        if (simPreset == null) {
            layoutDao.insertLayout(
                LayoutEntity(
                    id = "preset_sim",
                    name = "Elite Simulator Layout",
                    isPreset = true,
                    inputsJson = serializeInputs(LayoutPresets.getSimulationLayout())
                )
            )
        }
    }

    suspend fun saveCustomLayout(id: String, name: String, inputs: List<ControllerInput>) {
        layoutDao.insertLayout(
            LayoutEntity(
                id = id,
                name = name,
                isPreset = false,
                inputsJson = serializeInputs(inputs),
                lastModified = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCustomLayout(id: String) {
        layoutDao.deleteLayoutById(id)
    }
}
