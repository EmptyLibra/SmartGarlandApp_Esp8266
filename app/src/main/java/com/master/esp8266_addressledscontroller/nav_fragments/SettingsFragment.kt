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
import androidx.recyclerview.widget.LinearLayoutManager
import com.master.esp8266_addressledscontroller.R
import com.master.esp8266_addressledscontroller.databinding.FragmentSettingsBinding
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class SettingsFragment : Fragment(), EffectsListAdapter.Listener {
    private lateinit var  request: Request              // Класс для формирования http запросов
    private lateinit var binding: FragmentSettingsBinding
    private val client = OkHttpClient()
    private lateinit var pref: SharedPreferences        // Класс для сохранения значений (при закрытии приложения)
    private val ip: String = "192.168.1.17"

    private var isNeedSwitchListener : Boolean = true
    private var isNeedSColorListener : Boolean = true
    private var prevColor : Int = 0
    private var curTime  = System.currentTimeMillis()

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        //
        pref = activity?.getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)!!
        isNeedSColorListener = false

        colorPickerViewOnClickListener()  // Обработчик нажатий цветового круга
        connectSwitchOnClickListener()    // Обработчик переключателя состояния гирлянды

        //----------- Обработчики нажатий кнопок -----------
        binding.apply {
            // Настраиваем список эффектов
            effectsList.layoutManager = LinearLayoutManager(this@SettingsFragment.context)
            effectsList.adapter = EffectsListAdapter(this@SettingsFragment) // Адаптер для списка с эффектами
        }

        return binding.root
    }

    // Загрузка всех данных
    override fun onResume() {
        super.onResume()
        getFragmentData()
        post( "cmd?getAllConfig=1")
    }

    // Сохранение всех данных перед уничтожением фрагмента
    override fun onDestroyView() {
        super.onDestroyView()
        post( "cmd?saveAllConfig=1")
    }

    companion object{
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    // Обработчик нажатий на элементы списка с эффектами
    override fun effectListOnItemClick(effect: Effect) =with(binding) {
        if(switchConnect.isChecked) {
            post("effect?ef=mode${effect.index}") // Запуск эффекта

        }  else if(tvConnectStatus.text != "Статус: не отвечает!"){
            Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
        }
    }

    // Установка обработчика нажатий цветового круга
    @SuppressLint("SetTextI18n")
    private fun colorPickerViewOnClickListener() =with(binding){
        //----------- Цветовой круг ----------
        // Привязка ползунка яркости к кругу
        colorPickerWheel.attachBrightnessSlider(brightnessSlideBar)

        // Обработчик нажатий для Цветового круга
        colorPickerWheel.setColorListener(ColorEnvelopeListener { envelope, _ ->
            tvColor.text =
                "Red:${envelope.argb[1]}, Green: ${envelope.argb[2]}, Blue: ${envelope.argb[3]}\nЦвет: #${envelope.hexCode}"
            tvColor.setTextColor(envelope.color)

            if (!isNeedSColorListener) {
                isNeedSColorListener = true
                return@ColorEnvelopeListener
            }
            if(tvConnectStatus.text == "Статус: подключено!" && switchConnect.isChecked) {
                if ((prevColor != envelope.color) && (System.currentTimeMillis() - curTime > 100)) {
                    curTime  = System.currentTimeMillis()
                    prevColor = envelope.color
                    post("setBaseColor?color=${colorPickerWheel.colorEnvelope.hexCode}")
                }
            }
        })
    }

    // Установка обработчика переключателя состояния гирлянды
    private fun connectSwitchOnClickListener() = with(binding){
        //----------- Обработчик наждатий на ползунок состояния ----------
        switchConnect.setOnCheckedChangeListener { _, isChecked ->
            if(!isNeedSwitchListener) {
                isNeedSwitchListener = true
                return@setOnCheckedChangeListener
            }

            if(tvConnectStatus.text == "Статус: Ожидаем ответа...") {
                switchConnect.isChecked = !isChecked
                return@setOnCheckedChangeListener
            }

            tvConnectStatus.text = "Статус: Пдключение..."
            post( "cmd?setState=${if(isChecked) "1" else "0"}")
            switchConnect.text = if(isChecked) "Включена" else "Отключена"
        }
    }

    /* Сохраняем все данные фрагмента
    private fun saveFragmentData() =with(binding){
        val editor = pref.edit()
        editor.putString("answer", tvState.text.toString())

        //colorPickerWheel.setLifecycleOwner(this@SettingsFragment)
        editor.apply()
    } */

    // Получаем все данные фрагмента
    private fun getFragmentData() = with(binding) {
        val data = pref.getString("answer", "")
        if(data != null && data.isNotEmpty()) { tvState.text = data }
    }

    // Отправка команды на Esp и приём от неё ответа вида http://<destination_ip>/<command>?<param>=<value>
    @SuppressLint("SetTextI18n")
    fun post(command: String) = with(binding){

        // Отправляем запрос в отдельном потоке, т.к. это затратная операция
        Thread {
            // Формируем запрос вида: http://<destination_ip>/<command>?<param>=<value>
            request = Request.Builder().url("http://$ip/$command").build()
            Log.d("MY_LOGS", "Android: ${request.url()}")

            activity?.runOnUiThread {
                tvConnectStatus.text = "Статус: Ожидаем ответа..."
            }

            try{
                // Отправляем запрос
                val response = client.newCall(request).execute()
                if(response.isSuccessful) {
                    // Получаем ответ
                    val resultText = response.body()?.string()
                    Log.d("MY_LOGS", "Esp: $resultText")

                    activity?.runOnUiThread{
                        tvState.text = resultText
                        tvConnectStatus.text = "Статус: подключено!"

                        // Обработка ответа
                        if(command.contains("getAllConfig") && resultText != null) {
                            for(elem in resultText.split(";")) {
                                val arr = elem.split("=")
                                when(arr[0]) {
                                    // Установка состояния гирлянды
                                    "state" -> {
                                        isNeedSwitchListener = arr[1].toInt() != 1
                                        switchConnect.isChecked = arr[1].toInt() == 1
                                        switchConnect.text = if(arr[1].toInt() == 1) "Включена" else "Отключена"
                                    }

                                    // Установка цвета гирлянды
                                    "color" -> {
                                        isNeedSColorListener = false
                                        colorPickerWheel.selectByHsvColor(arr[1].toInt(16))
                                        tvColor.text = "Red:${colorPickerWheel.colorEnvelope.argb[1]}, " +
                                                "Green: ${colorPickerWheel.colorEnvelope.argb[2]}, " +
                                                "Blue: ${colorPickerWheel.colorEnvelope.argb[3]}\n" +
                                                "Цвет: #${colorPickerWheel.colorEnvelope.hexCode}"
                                        tvColor.setTextColor(colorPickerWheel.colorEnvelope.color)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        tvConnectStatus.text = "Статус: не отвечает!"
                        if(command.contains("setState")) {
                            isNeedSwitchListener = false
                            switchConnect.text = "Отключена"
                            switchConnect.isChecked = !switchConnect.isChecked
                        }
                        Toast.makeText(this@SettingsFragment.context, "Проверьте подключение к WiFi !", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("MY_LOGS", "Esp: NO ANSWER!")
                }
            } catch(i: IOException){
                activity?.runOnUiThread {
                    tvConnectStatus.text = "Статус: не отвечает!"
                    if(command.contains("setState")) {
                        isNeedSwitchListener = false
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