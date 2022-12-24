package com.master.esp8266_addressledscontroller.nav_fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.master.esp8266_addressledscontroller.LogElement
import com.master.esp8266_addressledscontroller.LogTypes
import com.master.esp8266_addressledscontroller.MainActivity
import com.master.esp8266_addressledscontroller.databinding.FragmentSettingsBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class SettingsFragment : Fragment(), EffectsListAdapter.Listener {
    private lateinit var  request: Request              // Класс для формирования http запросов
    private lateinit var binding: FragmentSettingsBinding
    private val client = OkHttpClient()
    private lateinit var pref: SharedPreferences        // Класс для сохранения значений (при закрытии приложения)
    private val ip: String = "192.168.1.17"

    private lateinit var colorPickerBuilder : ColorPickerDialog.Builder
    private var colorPickerDialog : ColorPickerView? = null
    private var colorPickerColor : Int = 0

    private var isNeedSwitchListener : Boolean = true
    private var isNeedSColorListener : Int = 2
    private var prevColor : Int = 0
    private var curTime  = System.currentTimeMillis()

    private var curEffectIndex = 0 // Индекс текущего эффекта

    private lateinit var mainActivity: MainActivity

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        mainActivity = activity as MainActivity

        //
        pref = activity?.getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)!!
        isNeedSColorListener = 2

        connectSwitchOnClickListener()    // Обработчик переключателя состояния гирлянды

        //----------- Обработчики нажатий кнопок -----------
        binding.apply {

            // Нажатие на кнопку отправки бегущей строки
            sendRunStr.setOnClickListener {
                if (editTextRunningStr.text.isNotEmpty()) {
                    if(switchConnect.isChecked) {
                        post("effect?ef=mode${10}&str=${editTextRunningStr.text}&isRainbow=${if(chBoxRainbowStr.isChecked) "1" else "0"}");
                    } else {
                        Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this@SettingsFragment.context, "Текстовое поле пусто\nВведите текст!", Toast.LENGTH_SHORT).show()
                }
            }

            tvEffectDelay.text = "Задержка эффекта:\n" + seekBarEffectDelay.progress.toString() + "мс"

            // Настраиваем список эффектов
            effectsList.layoutManager = GridLayoutManager(this@SettingsFragment.context, 2)
            effectsList.adapter = EffectsListAdapter(this@SettingsFragment) // Адаптер для списка с эффектами
            seekBarEffectDelay.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    tvEffectDelay.text = "Задержка эффекта:\n" + seekBarEffectDelay.progress.toString() + "мс"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if(switchConnect.isChecked) {
                        post("cmd?setCurEffectDelayMs=${seekBarEffectDelay.progress}")
                    } else {
                        Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
                    }
                }
            })
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
        curEffectIndex = effect.index
        seekBarEffectDelay.progress =  effect.standardDelay
        if(switchConnect.isChecked) {
            //post("cmd?getEffectDelayMs=${effect.systemName}") // Получаем задержку эффекта, устанавливаем её в slider
            post("effect?ef=mode${effect.index}")              // Запуск эффекта

        }  else if(tvConnectStatus.text != "Статус: не отвечает!"){
            Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
        }
    }

    // Создание диалогового окна с выбором цвета
    @SuppressLint("SetTextI18n")
    fun createColorPickerDialog() = with(binding) {
        isNeedSColorListener = 2

        // Создание сброрщика для View
        colorPickerBuilder = ColorPickerDialog.Builder(this@SettingsFragment.context)
            .setTitle("Выберите цвет и яркость")
            .setPositiveButton("Ок") { dialogInterface, _ -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true) // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last slide bar and buttons.

        // Получаем сам view и устанавливаем цвет
        colorPickerDialog = colorPickerBuilder.colorPickerView
        colorPickerDialog?.setInitialColor(colorPickerColor)

        // Устанавливаем слушатель нажатий на цветовой круг
        colorPickerDialog?.setColorListener(ColorEnvelopeListener { envelope, _ ->
            if (isNeedSColorListener != 0) {
                isNeedSColorListener--
                return@ColorEnvelopeListener
            }
            if(tvConnectStatus.text == "Статус: подключено!" && switchConnect.isChecked) {
                if ((prevColor != envelope.color) && (System.currentTimeMillis() - curTime > 100)) {
                    curTime  = System.currentTimeMillis()
                    prevColor = envelope.color
                    post("setBaseColor?color=${envelope.hexCode}")
                }
            }
        })
        colorPickerBuilder.show()
    }!!

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
        //if(data != null && data.isNotEmpty()) { tvState.text = data }
    }

    // Отправка команды на Esp и приём от неё ответа вида http://<destination_ip>/<command>?<param>=<value>
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun post(command: String) = with(binding){

        // Отправляем запрос в отдельном потоке, т.к. это затратная операция
        Thread {
            // Формируем запрос вида: http://<destination_ip>/<command>?<param>=<value>
            request = Request.Builder().url("http://$ip/$command").build()
            Log.d("MY_LOGS", "Android: ${request.url()}")
            mainActivity.logList.add(LogElement(LocalTime.now(), LogTypes.ANDROID_SEND, "Android: ${request.url()}"))

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
                    mainActivity.logList.add(LogElement(LocalTime.now(), LogTypes.ESP_ANSWER, "Esp: $resultText"))

                    activity?.runOnUiThread{
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
                                        isNeedSColorListener++
                                        colorPickerColor = arr[1].toInt(16)
                                    }
                                }
                            }
                        } else if(command.contains("setBaseColor") && resultText != null) {
                            if(colorPickerDialog != null)
                                colorPickerColor = colorPickerDialog!!.color

                        } else if(command.contains("getEffectDelayMs") && resultText != null) {
                            // Пришёл ответ о значении задержки эффекта
                            seekBarEffectDelay.progress = resultText.split("EffectDelay:")[1].toInt()
                        }
                    }
                } else {
                    //------ Нет ответа от МК ------
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
                    mainActivity.logList.add(LogElement(LocalTime.now(), LogTypes.ERROR, "Esp: NO ANSWER!"))
                }
            } catch(i: IOException){
                //------ Возникло исключение при отправке ------
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
                mainActivity.logList.add(LogElement(LocalTime.now(), LogTypes.ERROR, "EXCEPTION in get wi-fi answer!"))
            }

        }.start()
    }
}