package com.master.esp8266_addressledscontroller

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class MainActivityViewModel : ViewModel() {
    // Логи с отладочной информацией
    var logList = MutableLiveData<MutableList<LogElement>>()

    init{
        initLogList()
    }

    // Инициализация списка с отладочной информацией
    fun initLogList(){
        if(logList.value == null || logList.value!!.isEmpty()) {
            logList.value = mutableListOf(LogElement(LocalTime.now(),LogTypes.NOTE,
                "##########<br>Это консоль с отладочной информацией<br>##########<br>"))
        }
    }

    // Добавление элемента в логи
    fun addLog(elem: LogElement){
        logList.value?.add(elem)
    }
}