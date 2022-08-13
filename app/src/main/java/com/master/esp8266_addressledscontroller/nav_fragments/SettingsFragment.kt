package com.master.esp8266_addressledscontroller.nav_fragments

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.master.esp8266_addressledscontroller.R
import com.master.esp8266_addressledscontroller.databinding.FragmentSettingsBinding
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class SettingsFragment : Fragment() {
    private lateinit var  request: Request              // Класс для формирования http запросов
    private lateinit var binding: FragmentSettingsBinding
    private val client = OkHttpClient()
    private lateinit var pref: SharedPreferences        // Класс для сохранения значений (при закрытии приложения)
    private var ip: String = "192.168.1.17"

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        //
        pref = activity?.getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)!!
        binding.switchConnect.isChecked = true  // Включение гирлянды и получение ip
        post( "cmd","powerOn")

        binding.apply {

            switchConnect.setOnCheckedChangeListener { _, isChecked ->
                if(tvConnectStatus.text == "Статус: не отвечает") {
                    tvConnectStatus.text = "Статус: не отвечает!"
                    return@setOnCheckedChangeListener
                }

                if(tvConnectStatus.text == "Статус: Ожидаем ответа...") {
                    switchConnect.isChecked = !isChecked
                    return@setOnCheckedChangeListener
                }

                if(isChecked) {
                    tvConnectStatus.text = "Статус: Пдключение..."
                    post( "cmd","powerOn")
                    switchConnect.text = "Включена"
                } else {
                    post( "cmd","powerOff")
                    switchConnect.text = "Отключена"
                }

            }

            colorPickerWheel.attachBrightnessSlider(brightnessSlideBar)
            colorPickerWheel.setColorListener(ColorEnvelopeListener { envelope, _ ->
                tvColor.text = "Red:${envelope.argb[1]}, Green: ${envelope.argb[2]}, Blue: ${envelope.argb[3]}\n" +
                               "Цвет: #${envelope.hexCode}"

                tvColor.setTextColor(envelope.color)
            })

            btSendColor.setOnClickListener(onClickListener())
            bLed1.setOnClickListener(onClickListener())
            bLed2.setOnClickListener(onClickListener())
            bLed3.setOnClickListener(onClickListener())
        }

        return binding.root
    }

    override fun onPause() =with(binding){
        super.onPause()
        saveFragmentData()
    }

    override fun onResume() {
        super.onResume()
        getFragmentData()
    }

    companion object{
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when(it.id) {
                R.id.bLed1 -> { post("effect","mode1")}
                R.id.bLed2 -> { post("effect", "mode2")}
                R.id.bLed3 -> { post("effect","mode3")}//{ post("mode3")}

                R.id.btSendColor -> { post("setBaseColor", binding.tvColor.text.toString().substringAfter('#'))}
            }
        }
    }

    private fun saveFragmentData() =with(binding){
        val editor = pref.edit()
        editor.putString("answer", tvState.text.toString())
        editor.putString("ip", ip)

        colorPickerWheel.setLifecycleOwner(this@SettingsFragment)
        editor.apply()
    }

    private fun getFragmentData() = with(binding) {
        var data = pref.getString("ip", "")
        if(data != null && data.isNotEmpty()) { ip = data }

        data = pref.getString("answer", "")
        if(data != null && data.isNotEmpty()) { tvState.text = data }
    }

    // Отправка команды на Esp и приём от неё ответа
    @SuppressLint("SetTextI18n")
    private fun post(command: String, status: String = "") = with(binding){

        // Отправляем запрос в отдельном потоке, т.к. это затратная операция
        Thread {
            // Формируем запрос вида: http://<destination_ip>/<command>?<param>=<value>
            val sCommand: String = command + when(command) {
                "setBaseColor" -> "?color=$status"
                "effect" -> "?ef=$status"
                "cmd" -> "?cmd=$status"
                else -> ""
            }
            request = Request.Builder().url("http://$ip/$sCommand").build()
            Log.d("MY_LOGS", "Android: ${request.url()}")

            activity?.runOnUiThread {
                tvConnectStatus.text = "Статус: Ожидаем ответа..."
            }

            try{
                // Отправляем запрос
                var response = client.newCall(request).execute()
                if(response.isSuccessful) {
                    // Получаем ответ
                    val resultText = response.body()?.string()
                    Log.d("MY_LOGS", "Esp: $resultText")

                    activity?.runOnUiThread{
                        tvState.text = resultText
                        tvConnectStatus.text = "Статус: Подключено!"
                        if(status == "powerOn" || status == "powerOff") {
                            tvConnectStatus.text = tvConnectStatus.text.toString() + "\nip: $resultText"
                            if (resultText != null) {
                                ip = resultText
                            }
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        tvConnectStatus.text = "Статус: не отвечает"
                        if(status == "powerOff" || status == "powerOn") {
                            switchConnect.text = "Отключена"
                            switchConnect.isChecked = !switchConnect.isChecked
                        }
                        Toast.makeText(this@SettingsFragment.context, "Проверьте подключение к WiFi !", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("MY_LOGS", "Esp: NO ANSWER!")
                }
            } catch(i: IOException){
                activity?.runOnUiThread {
                    tvConnectStatus.text = "Статус: не отвечает"
                    if(status == "powerOff" || status == "powerOn") {
                        switchConnect.text = "Отключена"
                        switchConnect.isChecked = !switchConnect.isChecked
                    }
                    Toast.makeText(this@SettingsFragment.context, "Проверьте подключение к WiFi !", Toast.LENGTH_SHORT).show()
                }
                Log.d("MY_LOGS", "EXCEPTION in get wi-fi answer!")
            }

        }.start()
    }
}