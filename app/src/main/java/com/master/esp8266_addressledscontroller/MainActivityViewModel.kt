package com.master.esp8266_addressledscontroller

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.master.esp8266_addressledscontroller.main_tab_fragments.Effect

enum class ConnectStatus {
    NOT_CONNECT, IN_PROGRESS, RECEIVE_REQUEST, CONNECT, INIT_VALUE, ERROR_IN_COMMAND
}

enum class State {
    ENABLE, DISABLE
}

enum class McuCommand (var command: String){
    GET_ALL_CONFIG("getAllConfig"), SAVE_ALL_CONFIG("saveAllConfig"),
    CHANGE_EFFECT("changeEffect"), SET_BASE_COLOR("setBaseColor"),
    SET_AUTO_CHANGE_EFFECT_MODE("setAutoChangeEffectMode"),
    SET_CUR_EFFECT_DELAY_MS("setCurEffectDelayMs"),
    SET_LED_STRIP_STATE("setLedStripState"),
}

class MainActivityViewModel : ViewModel(){

    // Класс с конфигурациями светодиодной ленты
    data class LedStripConfig(
        var state:             MutableLiveData<State> = MutableLiveData(State.DISABLE),
        var autoModeState:     MutableLiveData<State> = MutableLiveData(State.DISABLE),
        var oneEffectDelaySec: MutableLiveData<Int>   = MutableLiveData(60),
        var curEffectIndex:    MutableLiveData<Int>   = MutableLiveData(0),
        var effectsList:       Array<Effect>          = arrayOf(),
        var curBaseColor:      Int                    = 0x00
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LedStripConfig

            if (state != other.state) return false
            if (autoModeState != other.autoModeState) return false
            if (oneEffectDelaySec != other.oneEffectDelaySec) return false
            if (curEffectIndex != other.curEffectIndex) return false
            if (!effectsList.contentEquals(other.effectsList)) return false
            if (curBaseColor != other.curBaseColor) return false

            return true
        }

        override fun hashCode(): Int {
            var result = state.hashCode()
            result = 31 * result + autoModeState.hashCode()
            result = 31 * result + oneEffectDelaySec.hashCode()
            result = 31 * result + curEffectIndex.hashCode()
            result = 31 * result + effectsList.contentHashCode()
            result = 31 * result + curBaseColor
            return result
        }

        fun toggleStripState(){
            state.value = if(state.value == State.ENABLE) State.DISABLE else State.ENABLE
        }

        fun toggleAutoModeState(){
            autoModeState.value =if(autoModeState.value == State.ENABLE) State.DISABLE else State.ENABLE
        }

        override fun toString(): String {
            return "state: ${state.value?.name}, autoModeState ${autoModeState.value?.name}, oneEffectDelaySec: ${oneEffectDelaySec.value}," +
                    " curEffectIndex: ${curEffectIndex.value}, curBaseColor: $curBaseColor"
        }
    }

    // Конфигцрация светодиодной ленты
    val stripConfig = LedStripConfig()
}