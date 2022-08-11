package com.master.esp8266_addressledscontroller

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.master.esp8266_addressledscontroller.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/* Для связи с МК по wi-fi надо:
* 1. Добавить в AndroidManifest разрешение на использование интернета и http.
* 2. Добавить в build.gradle (Module) библиотеку okhttp3
*
*Почитать о:
*  Тестирование мобильных приложений.
*
* */

class MainActivity : AppCompatActivity() {
    private lateinit var  request: Request              // Класс для формирования http запросов
    private lateinit var  binding: ActivityMainBinding
    private lateinit var pref: SharedPreferences        // Класс для сохранения значений (при закрытии приложения)
    private val client = OkHttpClient()

    // Функция создания активности (вызывается при её создании)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pref = getSharedPreferences("MyPref", MODE_PRIVATE)
        onClickSaveIp()
        getIp()
        binding.apply {
            bLed1.setOnClickListener(onClickListener())
            bLed2.setOnClickListener(onClickListener())
            bLed3.setOnClickListener(onClickListener())
        }

        Log.d("MY_LOGS", "Method: onCreate()")
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when(it.id) {
                R.id.bLed1 -> { post("mode1")}
                R.id.bLed2 -> { post("mode2")}
                R.id.bLed3 -> { post("mode3")}
            }
        }
    }

    private fun onClickSaveIp() = with(binding) {
        bSave.setOnClickListener {
            if(edip.text.isNotEmpty()) saveIp(edip.text.toString())
        }
    }

    // Сохранение ip в память
    private fun saveIp(ip: String) {
        val editor = pref.edit()
        editor.putString("ip", ip)
        editor.apply()
    }

    // Получение ip из памяти
    private fun getIp() = with(binding) {
        val ip = pref.getString("ip", "")
        if(ip != null && ip.isNotEmpty()) {
            edip.setText(ip)
        }
    }

    // Отправка команды на Esp и приём от неё ответа
    private fun post(post: String) {
        // Отправляем запрос в отдельном потоке, т.к. это затратная операция
        Thread {
            // Формируем запрос вида: http://<destination_ip>/<command>
            request = Request.Builder().url("http://${binding.edip.text}/effect?ef=$post").build()
            Log.d("MY_LOGS", "Android: ${request.url()}")

            try{
                // Отправляем запрос
                var response = client.newCall(request).execute()
                if(response.isSuccessful) {
                    // Получаем ответ
                    val resultText = response.body()?.string()
                    Log.d("MY_LOGS", "Esp: $resultText")

                    runOnUiThread{
                        binding.tvState.text = resultText
                    }
                } else {
                    Log.d("MY_LOGS", "Esp: NO ANSWER!")
                }
            } catch(i: IOException){
                Log.d("MY_LOGS", "EXCEPTION in get wi-fi answer!")
            }

        }.start()
    }
}