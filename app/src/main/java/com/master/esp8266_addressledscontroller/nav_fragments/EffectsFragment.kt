package com.master.esp8266_addressledscontroller.nav_fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.master.esp8266_addressledscontroller.LogElement
import com.master.esp8266_addressledscontroller.LogTypes
import com.master.esp8266_addressledscontroller.MainActivityViewModel
import com.master.esp8266_addressledscontroller.databinding.FragmentEffectsBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class EffectsFragment : Fragment(), EffectsListAdapter.Listener {

    // http данные
    private val client = OkHttpClient()                      // Объект клиента
    private val httpAddress: String = "http://192.168.1.17/" // Http адрес, на который посылаются команды
    private lateinit var request: Request                    // Пришедший ответ от контроллера

    private lateinit var colorPickerBuilder : ColorPickerDialog.Builder
    private var colorPickerDialog : ColorPickerView? = null
    private var colorPickerColor : Int = 0

    private var isNeedSwitchListener : Boolean = true
    private var isNeedSColorListener : Int = 2
    private var prevColor : Int = 0
    private var curTime  = System.currentTimeMillis()

    private var curEffectIndex = 0 // Индекс текущего эффекта

    // ViewModel главной активности
    private val mainViewModel: MainActivityViewModel by activityViewModels()

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var effectFragmentBinding: FragmentEffectsBinding

        @JvmStatic
        fun newInstance() = EffectsFragment()
    }

    /*###################### Методы цикла жизни фрагмента ############################*/
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        effectFragmentBinding = FragmentEffectsBinding.inflate(inflater, container, false)

        isNeedSColorListener = 2

        connectSwitchOnClickListener()    // Обработчик переключателя состояния гирлянды

        //----------- Обработчики нажатий кнопок -----------
        effectFragmentBinding.apply {
            testButton.setOnClickListener{
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/EmptyLibra/SmartGirlandApp_Esp8266/releases/download/v1.0.0-alpha/app-debug.apk")
                startActivity(intent)
                // Toast.makeText(this@EffectsFragment.context, "${BuildConfig.VERSION_NAME}", Toast.LENGTH_SHORT).show()
                // переходим на https://github.com/EmptyLibra/SmartGirlandApp_Esp8266/releases и ищем строки, начинающиеся с https и заканчивающиеся на .apk (проверяем первую найденую строку)
                // протестить на https://github.com/mozilla-mobile/fenix/releases
            }
            checkBoxAutoChangeEffects.setOnClickListener{
                if(editTextTextOneEffectTime.text.isEmpty()) {
                    post("cmd?setAutoChangeEffects=${( if(checkBoxAutoChangeEffects.isChecked) "1" else "0")}&setAutoCahngeEffectsMaxTime=${60000U}")
                }else {
                    val delayTime: Int = editTextTextOneEffectTime.text.toString().toInt()
                    post("cmd?setAutoChangeEffects=${( if(checkBoxAutoChangeEffects.isChecked) "1" else "0")}&setAutoCahngeEffectsMaxTime=${delayTime*1000}")
                }

            }

            // Нажатие на кнопку отправки бегущей строки
            sendRunStr.setOnClickListener {
                if (editTextRunningStr.text.isNotEmpty()) {
                    if(switchConnect.isChecked) {
                        post("effect?ef=mode${10}&str=${editTextRunningStr.text}&isRainbow=${if(chBoxRainbowStr.isChecked) "1" else "0"}")
                    } else {
                        Toast.makeText(this@EffectsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this@EffectsFragment.context, "Текстовое поле пусто\nВведите текст!", Toast.LENGTH_SHORT).show()
                }
            }

            tvEffectDelay.text = "Задержка эффекта:\n" + seekBarEffectDelay.progress.toString() + "мс"

            // Настраиваем список эффектов
            effectsList.layoutManager = GridLayoutManager(this@EffectsFragment.context, 2)
            effectsList.adapter = EffectsListAdapter(this@EffectsFragment) // Адаптер для списка с эффектами
            seekBarEffectDelay.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    tvEffectDelay.text = "Задержка эффекта:\n" + seekBarEffectDelay.progress.toString() + "мс"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if(switchConnect.isChecked) {
                        post("cmd?setCurEffectDelayMs=${seekBarEffectDelay.progress}")
                    } else {
                        Toast.makeText(this@EffectsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        return effectFragmentBinding.root
    }

    // OnViewCreated ???

    // Загрузка всех данных
    override fun onResume() {
        super.onResume()
        post( "cmd?getAllConfig=1")
    }

    // Сохранение всех данных перед уничтожением фрагмента
    override fun onDestroyView() {
        super.onDestroyView()
        post( "cmd?saveAllConfig=1")
    }

    /*###################### Методы обработки нажатий ############################*/
    // Обработчик нажатий на элементы списка с эффектами
    override fun effectListOnItemClick(effect: Effect) =with(effectFragmentBinding) {
        curEffectIndex = effect.index
        seekBarEffectDelay.progress =  effect.standardDelay
        if(switchConnect.isChecked) {
            //post("cmd?getEffectDelayMs=${effect.systemName}") // Получаем задержку эффекта, устанавливаем её в slider
            post("effect?ef=mode${effect.index}")              // Запуск эффекта

        }  else if(tvConnectStatus.text != "Статус: не отвечает!"){
            Toast.makeText(this@EffectsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
        }
    }

    // Создание диалогового окна с выбором цвета
    @SuppressLint("SetTextI18n")
    fun createColorPickerDialog() = with(effectFragmentBinding) {
        isNeedSColorListener = 2

        // Создание сброрщика для View
        colorPickerBuilder = ColorPickerDialog.Builder(this@EffectsFragment.context)
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
    private fun connectSwitchOnClickListener() = with(effectFragmentBinding){
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

    // Отправка команды на Esp и приём от неё ответа вида http://<destination_ip>/<command>?<param>=<value>
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun post(command: String) = with(effectFragmentBinding){
        // Отправляем запрос в отдельном потоке, т.к. это затратная операция
        Thread {
            // Формируем запрос вида: http://<destination_ip>/<command>?<param>=<value>
            request = Request.Builder().url(httpAddress + command).build()
            android.util.Log.d("MY_LOGS", "Android: ${request.url()}")
            mainViewModel.addLog(LogElement(LocalTime.now(), LogTypes.ANDROID_SEND, "Android: ${request.url()}"))

            activity?.runOnUiThread {
                tvConnectStatus.text = "Статус: Ожидаем ответа..."
            }

            try{
                // Отправляем запрос
                val response = client.newCall(request).execute()
                if(response.isSuccessful) {
                    // Получаем ответ
                    val resultText = response.body()?.string()
                    android.util.Log.d("MY_LOGS", "Esp: $resultText")
                    mainViewModel.addLog(LogElement(LocalTime.now(), LogTypes.ESP_ANSWER, "Esp: $resultText"))

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
                        Toast.makeText(this@EffectsFragment.context, "Проверьте подключение к WiFi !", Toast.LENGTH_SHORT).show()
                    }
                    android.util.Log.d("MY_LOGS", "Esp: NO ANSWER!")
                    mainViewModel.addLog(LogElement(LocalTime.now(), LogTypes.ERROR, "Esp: NO ANSWER!"))
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
                    Toast.makeText(this@EffectsFragment.context, "Проверьте подключение к WiFi !", Toast.LENGTH_SHORT).show()
                }
                android.util.Log.d("MY_LOGS", "EXCEPTION in get wi-fi answer!")
                mainViewModel.addLog(LogElement(LocalTime.now(), LogTypes.ERROR, "EXCEPTION in get wi-fi answer!"))
            }

        }.start()
    }
}