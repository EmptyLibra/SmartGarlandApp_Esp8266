package com.master.esp8266_addressledscontroller

import java.time.LocalTime
import java.time.format.DateTimeParseException

enum class LogTypes(val text: String){
    ERROR("Error"), ANDROID_SEND("Android_Send"), ESP_ANSWER("Esp_Answer"),
    NOTE("NOTE")
}

data class LogElement(var Date: LocalTime, var Type: LogTypes, var message: String) {

    override fun toString(): String {
        val str = "${Date.hour}:${Date.minute}:${Date.second} [${Type.text}]:<br> $message"
        return when(this.Type){
            LogTypes.ERROR -> "<font color=\"red\">$str</font><br><br>"
            LogTypes.ANDROID_SEND -> "<font color=\"blue\">$str</font><br><br>"
            LogTypes.ESP_ANSWER -> "<font color=\"green\">$str</font><br><br>"
            LogTypes.NOTE -> "<font color=\"gray\">$str</font><br><br>"
        }
    }

    companion object{
        fun fromString(str: String): LogElement {
            if(str == "") return LogElement(LocalTime.now(), LogTypes.ERROR, "Error convert from string to Log Element")

            var strWithoutHtml = str.replace(Regex(""".*>.*</font>"""), "")


            val date = try {
                LocalTime.parse(strWithoutHtml.substringBefore(" [", ""))
            } catch(e: DateTimeParseException) {
                MainActivity.logList.add(LogElement(LocalTime.now(), LogTypes.ERROR, "Exception in parsing string to Date (function: fromString)"))
                LocalTime.MIN
            }
            strWithoutHtml = strWithoutHtml.replace(Regex(""".* \["""), "")

            val type = try {
                LogTypes.valueOf(strWithoutHtml.substringBefore("]:", ""))
            } catch(e: IllegalArgumentException ) {
                MainActivity.logList.add(LogElement(LocalTime.now(), LogTypes.ERROR, "Exception in parsing string to LogTypes (function: fromString)"))
                LogTypes.ERROR
            }
            strWithoutHtml = strWithoutHtml.replace(Regex(""".*]:<br> """), "")

            return LogElement(date, type, strWithoutHtml)
        }
    }

}
