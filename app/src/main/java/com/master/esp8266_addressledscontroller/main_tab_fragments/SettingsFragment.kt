package com.master.esp8266_addressledscontroller.main_tab_fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.master.esp8266_addressledscontroller.*
import com.master.esp8266_addressledscontroller.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {
    // Привязка к Layout фрагмента
    private lateinit var fragMainBinding: FragmentSettingsBinding

    // ViewModel главной активности
    private val mainViewModel: MainActivityViewModel by activityViewModels()

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

        setViewsEnabled(false)

        /*====================== Прослушивание изменений у LiveData =========================*/
        observeConnectToMcuStatus() // Наблюдатель за состоянием статуса подключения
        observeStripState()         // Наблюдатель за состоянием гирлянды (включена, выключена)
        observeAutoModeState()      // Наблюдатель за состоянием авто-смены эффектов

        /*====================== Обработчики нажатий =========================*/
        setSwitchConnectOnClickListener()     // Для переключателя состояния гирлянды
        setRadioButtonsOnClickListener()      // Для радио-кнопок авто-режима
        setButtonSendRunStrOnClickListener()  // Для кнопки отправки бегущей строки
        setSeekBarEffectSpeedListener()       // Для ползунка скорости эффекта

        return fragMainBinding.root
    }

    // Загрузка всех данных
    override fun onResume() {
        super.onResume()
        setViewsEnabled(false)
        httpHandler.post( "cmd?${McuCommand.GET_ALL_CONFIG.command}=1")
    }

    // Сохранение всех данных перед уничтожением фрагмента
    override fun onDestroy() {
        super.onDestroy()
        httpHandler.post( "cmd?${McuCommand.SAVE_ALL_CONFIG.command}=1")
    }

    /*###################### Обработчики нажатий ############################*/
    // Назначение слушателя нажатий для переключателя состояния гирлянды
    private fun setSwitchConnectOnClickListener() = with(fragMainBinding){
        switchConnect.setOnCheckedChangeListener { _, isChecked ->
            if(HttpHandler.lastCommand == "") return@setOnCheckedChangeListener

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

            if(!radioButtonAutoChangeEn.isEnabled) return@setOnCheckedChangeListener

            if(MainActivity.connectToMcuStatus.value == ConnectStatus.IN_PROGRESS ||
                        mainViewModel.stripConfig.state.value == State.DISABLE)
            {
                radioButtonAutoChangeEn.isActivated = !radioButtonAutoChangeEn.isActivated
                radioButtonAutoChangeDis.isActivated = !radioButtonAutoChangeDis.isActivated

                if(mainViewModel.stripConfig.state.value == State.DISABLE) {
                    Toast.makeText(this@SettingsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
                }
                return@setOnCheckedChangeListener
            }

            if (editTextTextOneEffectTime.text.isEmpty()) {
                httpHandler.post("cmd?${McuCommand.SET_AUTO_CHANGE_EFFECTS_MODE.command}=" +
                        (if (radioButtonAutoChangeEn.isChecked) "1" else "0") +
                        "&setAutoCahngeEffectsMaxTime=${60000U}")
            } else {
                val delayTime: Int = editTextTextOneEffectTime.text.toString().toInt()
                httpHandler.post("cmd?${McuCommand.SET_AUTO_CHANGE_EFFECTS_MODE.command}=" +
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
                ConnectStatus.NOT_CONNECT -> tvConnectStatus.text = "Статус: не отвечает"
                ConnectStatus.ERROR_IN_COMMAND -> tvConnectStatus.text = "Статус: ошибка в команде!"
                else -> {}
            }

            setViewsEnabled(it == ConnectStatus.CONNECT && mainViewModel.stripConfig.state.value == State.ENABLE)
            when(it) {
                ConnectStatus.NOT_CONNECT, ConnectStatus.ERROR_IN_COMMAND -> {
                    when(HttpHandler.lastCommand) {
                        McuCommand.SET_LED_STRIP_STATE.command -> toggleSwitchConnect()
                        McuCommand.SET_AUTO_CHANGE_EFFECTS_MODE.command -> toggleRadioButtons()
                    }
                }
                else -> {}
            }
        }
    }

    // Наблюдатель за состоянием гирлянды (включена, выключена)
    private fun observeStripState() =with(fragMainBinding){
        mainViewModel.stripConfig.state.observe(viewLifecycleOwner) {
            setViewsEnabled(it == State.ENABLE)
            if(MainActivity.connectToMcuStatus.value == ConnectStatus.CONNECT) {
                switchConnect.setOnCheckedChangeListener(null)
                when(it) {
                    State.ENABLE -> switchConnect.isChecked = true
                    State.DISABLE -> switchConnect.isChecked = false
                    else -> {}
                }
                switchConnect.text = if(switchConnect.isChecked) "Выключить" else "Включить"
                setSwitchConnectOnClickListener()
            }
        }
    }

    // Наблюдатель за состоянием авто-смены эффектов
    private fun observeAutoModeState() =with(fragMainBinding){
        mainViewModel.stripConfig.autoModeState.observe(viewLifecycleOwner) {
            // Изменяем состояние переключателей RadioButton
            if(MainActivity.connectToMcuStatus.value == ConnectStatus.CONNECT) {
                radioGroup.setOnCheckedChangeListener(null)
                when(it) {
                    State.DISABLE -> radioGroup.check(radioButtonAutoChangeDis.id)
                    State.ENABLE -> radioGroup.check(radioButtonAutoChangeEn.id)
                    else -> {}
                }
                setRadioButtonsOnClickListener()
            }
        }
    }

    /*==================== Вспомогательные функции =========================*/
    // Переключить обратко radioButtons, ответственные за авто-режим
    private fun toggleRadioButtons() = with(fragMainBinding){
        radioGroup.setOnCheckedChangeListener(null)
        when(radioGroup.checkedRadioButtonId) {
            radioButtonAutoChangeDis.id -> radioGroup.check(radioButtonAutoChangeEn.id)
            radioButtonAutoChangeEn.id -> radioGroup.check(radioButtonAutoChangeDis.id)
        }
        setRadioButtonsOnClickListener()
    }

    // Переключить обратко switch, ответственный за состояние ленты
    private fun toggleSwitchConnect() = with(fragMainBinding){
        switchConnect.setOnCheckedChangeListener(null)
        switchConnect.isChecked = !switchConnect.isChecked
        switchConnect.text = if(switchConnect.isChecked) "Выключить" else "Включить"
        setSwitchConnectOnClickListener()
    }

    // Установка состояний у радио-кнопок авто-режима
    private fun setViewsEnabled(state : Boolean) =with(fragMainBinding){
        radioButtonAutoChangeEn.isEnabled = state
        radioButtonAutoChangeDis.isEnabled = state
        seekBarEffectDelay.isEnabled = state

        sendRunStr.isEnabled = state
    }
}