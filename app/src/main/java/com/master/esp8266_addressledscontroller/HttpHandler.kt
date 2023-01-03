package com.master.esp8266_addressledscontroller

import android.annotation.SuppressLint
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class HttpHandler {
    private val client = OkHttpClient()                      // Объект клиента
    private val httpAddress: String = "http://192.168.1.17/" // Http адрес, на который посылаются команды
    private lateinit var request: Request                    // Пришедший ответ от контроллера

    companion object {
        var requestMessage: String = ""

        var lastCommand: String = ""

        // Экземпляр фрагмента создастся только один раз, а дальше будет браться уже существующий
        @JvmStatic
        fun newInstance() = HttpHandler()
    }

    // Отправка команды на Esp и приём от неё ответа вида http://<destination_ip>/<command>?<param>=<value>
    @SuppressLint("SetTextI18n")
    fun post(command: String) {
        // Отправляем запрос в отдельном потоке, т.к. это затратная операция
        Thread {
            // Формируем запрос вида: http://<destination_ip>/<command>?<param>=<value>
            request = Request.Builder().url(httpAddress + command).build()
            writeStrToLogs(LogTypes.ANDROID_SEND, "Android: ${request.url()}")

            MainActivity.connectToMcuStatus.postValue(ConnectStatus.IN_PROGRESS)

            try {
                // Отправляем запрос
                val response = client.newCall(request).execute()

                // Принимаем ответ
                receiveRequest(command, response)

            } catch (i: IOException) {
                lastCommand = command.substringBefore("=").substringAfter("?", "")

                // Возникло исключение при отправке, устанавливаем статус
                MainActivity.connectToMcuStatus.postValue(ConnectStatus.NOT_CONNECT)

                writeStrToLogs(LogTypes.ERROR, "EXCEPTION in get wi-fi answer!")
            }
        }.start()
    }

    // Принимаем ответ
    private fun receiveRequest(command: String, response: Response) {
        val temp = command.substringBefore("=").substringAfter("?", "")
        lastCommand = if(temp == McuCommand.SAVE_ALL_CONFIG.command) "" else temp

        if(response.isSuccessful) {
            requestMessage = response.body()?.string().toString()

            if(requestMessage.contains("Unknown Command")) {
                MainActivity.addToLogList(LogTypes.ERROR, "ESP receive Unknown Command!<br> $requestMessage")
                MainActivity.connectToMcuStatus.postValue(ConnectStatus.ERROR_IN_COMMAND)
            } else {
                MainActivity.connectToMcuStatus.postValue(ConnectStatus.RECEIVE_REQUEST)
            }

            writeStrToLogs(LogTypes.ESP_ANSWER, "Esp: $requestMessage")

        } else {
            MainActivity.connectToMcuStatus.postValue(ConnectStatus.NOT_CONNECT)
            MainActivity.addToLogList(LogTypes.ERROR, "Esp: NO ANSWER!")
        }
    }

    private fun writeStrToLogs(LogType: LogTypes, message: String){
        Log.d("MY_LOGS", message)
        MainActivity.addToLogList(LogType, message)
    }
}