package com.master.esp8266_addressledscontroller

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalTime

enum class LogTypes(val text: String){
    ERROR("Error"), ANDROID_SEND("Android_Send"), ESP_ANSWER("Esp_Answer"),
    NOTE("NOTE")
}

data class LogElement(var Date: LocalTime, var Type: LogTypes, var message: String) {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun toString(): String {
        val str = "${Date.hour}:${Date.minute}:${Date.second} [${Type.text}]:<br> $message"
        return when(this.Type){
            LogTypes.ERROR -> "<font color=\"red\">$str</font><br><br>"
            LogTypes.ANDROID_SEND -> "<font color=\"blue\">$str</font><br><br>"
            LogTypes.ESP_ANSWER -> "<font color=\"green\">$str</font><br><br>"
            LogTypes.NOTE -> "<font color=\"gray\">$str</font><br><br>"
        }
    }
}
