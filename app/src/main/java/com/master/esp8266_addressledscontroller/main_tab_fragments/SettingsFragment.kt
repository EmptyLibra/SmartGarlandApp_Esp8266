package com.master.esp8266_addressledscontroller.main_tab_fragments

import android.R.id.text1
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.master.esp8266_addressledscontroller.*
import com.master.esp8266_addressledscontroller.databinding.FragmentSettingsBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection


class SettingsFragment : Fragment() {
    // Привязка к Layout фрагмента
    private lateinit var fragMainBinding: FragmentSettingsBinding

    // ViewModel главной активности
    private val mainViewModel: MainActivityViewModel by viewModels()

    // Обработчик http запросов
    private val httpHandler = HttpHandler.newInstance()

    companion object{
        // Экземпляр фрагмента создастся только один раз, а дальше будет браться уже существующий
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    /*###################### Методы цикла жизни фрагмента ############################*/
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Привязка Layout к фрагменту
        fragMainBinding = FragmentSettingsBinding.inflate(inflater, container, false)

        /*====================== Прослушивание изменений у LiveData =========================*/
        observeConnectToMcuStatus() // Наблюдатель за состоянием статуса подключения
        observeStripState()         // Наблюдатель за состоянием гирлянды (включена, выключена)
        observeAutoModeState()      // Наблюдатель за состоянием авто-смены эффектов

        /*====================== Обработчики нажатий =========================*/
        setSwitchConnectOnClickListener()     // Для переключателя состояния гирлянды
        setRadioButtonsOnClickListener()      // Для радио-кнопок авто-режима
        setButtonSendRunStrOnClickListener()  // Для кнопки отправки бегущей строки
        setSeekBarEffectSpeedListener()       // Для ползунка скорости эффекта

        fragMainBinding.testButton.setOnClickListener{
            Thread {
                try {
                    val url = URL("https://github.com/EmptyLibra/SmartGirlandApp_Esp8266/releases")
                    val con1: URLConnection = url.openConnection()
                    val reader = BufferedReader(InputStreamReader(con1.getInputStream()))
                    Log.d("MY_LOGS", reader.readText())
                    //Log.d("MY_LOGS", Regex("""https://.*.apk""").find(reader.readText()).toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            val textView = TextView(activity)
            textView.text = "Доступна новая версия приложения!"
            textView.gravity = Gravity.CENTER
            textView.textSize = 20.0F

            val message ="Новая версия: v6.6.6\n" +
                    "Текущая версия: ${BuildConfig.VERSION_NAME}\n" +
                    "Хотите обновить приложение?"

            val dialog = AlertDialog.Builder(it.context)
                .setCustomTitle(textView)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Да") { dialog, _ ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse("https://github.com/EmptyLibra/SmartGirlandApp_Esp8266/releases/download/v1.0.0-alpha/app-debug.apk")
                    startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("Позже") { dialog, _ ->
                    dialog.cancel()
                }
                .create()
            dialog.show()

            val parent = dialog.getButton(AlertDialog.BUTTON_NEGATIVE).parent as LinearLayout
            parent.gravity = Gravity.CENTER_HORIZONTAL
            val leftSpacer = parent.getChildAt(1)
            leftSpacer.visibility = View.GONE
            // Toast.makeText(this@EffectsFragment.context, "${BuildConfig.VERSION_NAME}", Toast.LENGTH_SHORT).show()
            // переходим на https://github.com/EmptyLibra/SmartGirlandApp_Esp8266/releases и ищем строки, начинающиеся с https и заканчивающиеся на .apk (проверяем первую найденую строку)
            // протестить на https://github.com/mozilla-mobile/fenix/releases
        }

        return fragMainBinding.root
    }

    // Загрузка всех данных
    override fun onResume() {
        super.onResume()
        MainActivity.connectToMcuStatus.value = ConnectStatus.INIT_VALUE
        httpHandler.post( "cmd?${McuCommand.GET_ALL_CONFIG.command}=1")
    }

    override fun onDestroy() {
        super.onDestroy()
        HttpHandler.lastCommand = ""
    }

    /*###################### Обработчики нажатий ############################*/
    // Назначение слушателя нажатий для переключателя состояния гирлянды
    private fun setSwitchConnectOnClickListener() = with(fragMainBinding){
        switchConnect.setOnCheckedChangeListener { _, isChecked ->
            Log.d("MY_LOGS", " Switch check!!")
            if(MainActivity.connectToMcuStatus.value == ConnectStatus.IN_PROGRESS) {
                switchConnect.isChecked = !isChecked
                return@setOnCheckedChangeListener
            }

            httpHandler.post( "cmd?${McuCommand.SET_LED_STRIP_STATE.command}=${if(isChecked) "1" else "0"}")
            switchConnect.text = if(isChecked) "Выключить" else "Включить"
        }
    }

    // Назначение слушателя нажатий для радио-кнопок авторежим
    private fun setRadioButtonsOnClickListener() =with(fragMainBinding){
        radioGroup.setOnCheckedChangeListener{ _, _ ->
            if(MainActivity.connectToMcuStatus.value == ConnectStatus.IN_PROGRESS) {
                when(radioGroup.checkedRadioButtonId) {
                    radioButtonAutoChangeDis.id -> radioGroup.check(radioButtonAutoChangeEn.id)
                    radioButtonAutoChangeEn.id -> radioGroup.check(radioButtonAutoChangeDis.id)
                }
                return@setOnCheckedChangeListener

            } else if(mainViewModel.stripConfig.state.value == State.ENABLE) {
                Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
            }

            if (editTextTextOneEffectTime.text.isEmpty()) {
                httpHandler.post("cmd?${McuCommand.SET_AUTO_CHANGE_EFFECT_MODE.command}=" +
                        (if (radioButtonAutoChangeEn.isChecked) "1" else "0") +
                        "&setAutoCahngeEffectsMaxTime=${60000U}")
            } else {
                val delayTime: Int = editTextTextOneEffectTime.text.toString().toInt()
                httpHandler.post("cmd?${McuCommand.SET_AUTO_CHANGE_EFFECT_MODE.command}=" +
                        (if (radioButtonAutoChangeEn.isChecked) "1" else "0") +
                        "&setAutoCahngeEffectsMaxTime=${delayTime * 1000}")
            }
        }
    }

    // Установка слушателя нажатий для кнопки отправки бегущей строки
    private fun setButtonSendRunStrOnClickListener() =with(fragMainBinding){
        sendRunStr.setOnClickListener {
            if (editTextRunningStr.text.isNotEmpty()) {
                if(mainViewModel.stripConfig.state.value == State.ENABLE) {
                    httpHandler.post("${McuCommand.CHANGE_EFFECT.command}?ef=mode${10}&str=${editTextRunningStr.text}&isRainbow=${if(chBoxRainbowStr.isChecked) "1" else "0"}")
                } else {
                    Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this@SettingsFragment.context, "Текстовое поле пусто\nВведите текст!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Установка слушателя нажатий для ползунка, отвечающего за скорость эффекта
    private fun setSeekBarEffectSpeedListener() =with(fragMainBinding){
        var tempString = "Скорость эффекта:\n" + seekBarEffectDelay.progress.toString() + "мс"
        tvEffectDelay.text = tempString

        seekBarEffectDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                tempString = "Скорость эффекта:\n" + seekBarEffectDelay.progress.toString() + "мс"
                tvEffectDelay.text = tempString
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if(mainViewModel.stripConfig.state.value == State.ENABLE) {
                    httpHandler.post("cmd?${McuCommand.SET_CUR_EFFECT_DELAY_MS.command}=${seekBarEffectDelay.progress}")
                } else {
                    Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /*###################### Наблюдатели за состояниями переменных ############################*/
    // Наблюдатель за состоянием статуса подключения
    private fun observeConnectToMcuStatus() =with(fragMainBinding){
        MainActivity.connectToMcuStatus.observe(viewLifecycleOwner) {
            when(it) {
                ConnectStatus.IN_PROGRESS -> tvConnectStatus.text = "Статус: ожидание ответа..."
                ConnectStatus.CONNECT     -> tvConnectStatus.text = "Статус: подключён"
                ConnectStatus.NOT_CONNECT, ConnectStatus.ERROR_IN_COMMAND -> {
                    tvConnectStatus.text = if(MainActivity.connectToMcuStatus.value == ConnectStatus.NOT_CONNECT)
                        "Статус: не подключён" else "Статус: ошибка в команде!"

                    Log.d("MY_LOGS", "Connect to MCU change. New ConnectState: ${MainActivity.connectToMcuStatus.value}")

                    when(HttpHandler.lastCommand) {
                        McuCommand.SET_LED_STRIP_STATE.command -> toggleSwitchConnect()
                        McuCommand.SET_AUTO_CHANGE_EFFECT_MODE.command -> toggleRadioButtons()
                    }
                }
                else -> {}
            }
        }
    }

    // Наблюдатель за состоянием гирлянды (включена, выключена)
    private fun observeStripState(){
        mainViewModel.stripConfig.state.observe(viewLifecycleOwner) {

            Log.d("MY_LOGS", "Strip state change. New state: ${mainViewModel.stripConfig.state.value} : mcuStatus ${MainActivity.connectToMcuStatus.value}")

            if((it == State.DISABLE || it == State.ENABLE) &&
                MainActivity.connectToMcuStatus.value == ConnectStatus.CONNECT) {
                toggleSwitchConnect()
            }
        }
    }

    // Наблюдатель за состоянием авто-смены эффектов
    private fun observeAutoModeState(){
        mainViewModel.stripConfig.autoModeState.observe(viewLifecycleOwner) {

            Log.d("MY_LOGS", "AutoMode change. New state: ${mainViewModel.stripConfig.autoModeState.value} : mcuStatus ${MainActivity.connectToMcuStatus.value}")

            // Изменяем состояние переключателей RadioButton
            if(((it == State.DISABLE) || (it == State.ENABLE)) &&
                MainActivity.connectToMcuStatus.value == ConnectStatus.CONNECT)
            {
                toggleRadioButtons()
            }
        }
    }

    /*==================== Вспомогательные функции =========================*/
    // Переключить обратко radioButtons, ответственные за авто-режим
    private fun toggleRadioButtons() = with(fragMainBinding){
        Log.d("MY_LOGS", " Toggle radioButons!")
        radioGroup.setOnCheckedChangeListener(null)
        when(radioGroup.checkedRadioButtonId) {
            radioButtonAutoChangeDis.id -> radioGroup.check(radioButtonAutoChangeEn.id)
            radioButtonAutoChangeEn.id -> radioGroup.check(radioButtonAutoChangeDis.id)
        }
        setRadioButtonsOnClickListener()
    }

    // Переключить обратко switch, ответственный за состояние ленты
    private fun toggleSwitchConnect() = with(fragMainBinding){
        Log.d("MY_LOGS", " Toggle switch connect!")
        switchConnect.setOnCheckedChangeListener(null)
        switchConnect.isChecked = !switchConnect.isChecked
        switchConnect.text = if(switchConnect.isChecked) "Выключить" else "Включить"
        setSwitchConnectOnClickListener()
    }
}