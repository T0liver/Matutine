package hu.toliver.matutine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionTranscript
import hu.toliver.matutine.edition.LightEditField
import hu.toliver.matutine.edition.applyLightEdit
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.encodeEdition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WebEditionModel(
    private val token: String,
    initial: Edition,
    private val scope: CoroutineScope,
) {
    var edition by mutableStateOf(initial)
        private set
    var lightEditSaved by mutableStateOf(false)
        private set
    var lightEditError by mutableStateOf<String?>(null)
        private set
    var lightEditResetKey by mutableIntStateOf(0)
        private set

    private var saveGeneration = 0

    fun commit(field: LightEditField, value: String) {
        val updated = applyLightEdit(edition, field, value)
        if (updated == edition) {
            lightEditError = null
            return
        }
        val generation = ++saveGeneration
        scope.launch {
            val response = apiPut("/editions/by-id", token, encodeEdition(updated))
            if (generation != saveGeneration) return@launch
            if (response.status == 200) {
                edition = decodeEdition(response.body)
                lightEditError = null
                lightEditSaved = true
            } else {
                lightEditError = EditionTranscript.SAVE_FAILED
            }
        }
    }

    fun discardLightEdit() {
        saveGeneration += 1
        lightEditError = null
        lightEditResetKey += 1
    }

    fun replaceEdition(next: Edition) {
        edition = next
    }
}
