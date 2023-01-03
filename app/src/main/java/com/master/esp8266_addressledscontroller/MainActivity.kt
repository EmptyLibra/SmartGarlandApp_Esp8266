package com.master.esp8266_addressledscontroller

import android.R.id
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayout
import com.master.esp8266_addressledscontroller.databinding.ActivityMainBinding
import com.master.esp8266_addressledscontroller.main_tab_fragments.EffectsFragment
import com.master.esp8266_addressledscontroller.main_tab_fragments.SettingsFragment
import com.master.esp8266_addressledscontroller.side_nav_fragments.DrawingFragment
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.time.LocalTime


/* Список дел:
* 1. Реализовать запрос всех эффектов с МК.
* 2. Отображать информацию о текущем запущенном эффекте.
* 3. ViewPager2
* 4. Перенести задержку эффектов на дрругой фрагмент
* 5. Реализовать отправку данных с МК, если кто-то другой изменил конфигурации ленты (динамическое отслеживание параметров)
*
* ==================
* Полезные сочетания клавиш:
* ctrl+D - быстро скопировать
* ctrl+shift+- - быстро свернуть все функции
* ctrl+alt+o - оптимизация импорта
* Shift + F6 - переименовать переменную и все её упоминания
* Ctrl + Shift + F6 - поиск по всем файлам
* Ctrl + Alt + O - оптимизация импорта
*
* ==================
* Запуск отладки по wifi: .\StartWifiDebug.bat
* Почитать о:
* Тестирование мобильных приложений.
*
* */

class MainActivity : AppCompatActivity() {
    private lateinit var  mainBinding: ActivityMainBinding           // Класс привязки к xml файлу
    private val mainViewModel: MainActivityViewModel by viewModels() // ViewModel активности

    // Обработчик http запросов
    private val httpHandler = HttpHandler.newInstance()

    // Состояния view ColorPicker с выбором цвета и текущее время
    private var prevColorPickerColor : Int = 0
    private var curTime  = System.currentTimeMillis()

    // Список с фрагментами из главного меню
    private val tabFragList = listOf(
        SettingsFragment.newInstance(),
        EffectsFragment.newInstance()
    )

    companion object {
        // Статус подключения к микроконтроллеру
        var connectToMcuStatus = MutableLiveData(ConnectStatus.INIT_VALUE)

        // Логи с отладочной информацией
        private val logList = mutableListOf(LogElement(LocalTime.now(),LogTypes.NOTE,
                "##########<br>Это консоль с отладочной информацией<br>##########<br>"))

        fun addToLogList(logType: LogTypes, message: String){
            if(logList.size < 1000) {
                logList.add(LogElement(LocalTime.now(), logType, message))
            } else {
                logList.clear()
                logList.add(LogElement(LocalTime.now(),LogTypes.NOTE,
                    "##########<br>Это консоль с отладочной информацией<br>##########<br>"))
            }
        }
    }

    /*###################### Методы жизненного цикла активности ############################*/
    // Функция создания активности (вызывается при её создании)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение контента из xml файла
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        // Помещаем новый фрагмент, если это первое создание активности
        if(savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(R.id.content_main, SettingsFragment.newInstance(), "Home fragment").commit()
        }

        /*====================== Прослушивание изменений у LiveData =========================*/
        // Подписываемся на прослушивание статуса подключения к контроллеру
        observeConnectToMcuStatus()

        /*====================== Обработчики нажатий =========================*/
        // Обработчик нажатий на элементы TаbLayout в домашнем фрагменте
        mainBinding.homeTabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            // Переключаемся на новый фрагмент при нажатии
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    supportFragmentManager.beginTransaction().replace(R.id.content_main, tabFragList[tab.position], "Home fragment").commit()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })

        // ToolBar
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_home)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Встроенная стрелка возвращения
        supportActionBar?.title = "Умная гирлянда"         // Название в ToolBar

        // Обработчик пунктов выдвижного меню
        setNavItemSelectListener()

        // Проверка наличия обновления
        checkForUpdate()
    }

    // Сохранение всех данных перед уничтожением фрагмента
    override fun onDestroy() {
        super.onDestroy()
        httpHandler.post( "cmd?${McuCommand.SAVE_ALL_CONFIG.command}=1")
    }

    // Сохранение данных при повороте экрана
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString("Connect Status", connectToMcuStatus.value.toString())

        outState.putInt("Log list size", logList.size)
        for((index, elem) in logList.withIndex()){
            outState.putString("LogElem$index", elem.toString())
        }
    }

    // Восстанавление данных
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        connectToMcuStatus.value =  ConnectStatus.valueOf(savedInstanceState.getString("Connect Status") ?: ConnectStatus.NOT_CONNECT.toString())

        var logListSize = savedInstanceState.getInt("Log list size", 0)

        if(logListSize > 1000) logListSize = 1000

        for(index in 0..logListSize){
            logList.add(LogElement.fromString(savedInstanceState.getString("LogElem$index", "")))
        }

    }

    /*###################### Обработчики нажатий ############################*/
    // Создание меню ToolBar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        // Если текущий фрагмент не домашний, то скрываем значок выбора цвета
        menu.findItem(R.id.change_color).isVisible = supportFragmentManager.findFragmentByTag("Home fragment") != null
        return true
    }

    // Обработка нажатий кнопок на ToolBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean = with(mainBinding) {
        when(item.itemId){
            // Кнопка домой
            id.home -> {
                if(drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START)
                } else {
                    drawer.openDrawer(GravityCompat.START)
                }
            }

            // Кнопка изменения цвета
            R.id.change_color -> {
                // Запуск окна с выбором цвета
                createColorPickerDialog()
            }

            // Кнопка с Логами
            R.id.logs -> {
                val diaLogs = Dialog(this@MainActivity)
                diaLogs.setTitle("Логи")
                diaLogs.setContentView(R.layout.logs_layout)

                val textView = diaLogs.findViewById<TextView>(R.id.textViewLogs)
                textView.movementMethod = ScrollingMovementMethod()

                var text = ""
                for(elem in logList) {
                    text += elem.toString()
                }
                textView.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                textView.setBackgroundColor(Color.BLACK)
                textView.gravity = Gravity.BOTTOM

                val closeButton = diaLogs.findViewById<TextView>(R.id.butCloseColorPickerDialog)
                closeButton.setOnClickListener{
                    diaLogs.cancel()
                }

                diaLogs.show()
            }
        }
        return true
    }

    // Обработчик пунктов выдвижного меню
    private fun setNavItemSelectListener() = with(mainBinding){
        nvMenu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_settings -> {
                    supportActionBar?.title = "Умная гирлянда"

                    // Запуск нового фрагмента
                    supportFragmentManager.beginTransaction().replace(R.id.content_main,
                        EffectsFragment.newInstance(), "Home fragment").commit()
                }
                R.id.nav_draw -> {
                    supportActionBar?.title = "Рисовалка"
                    supportFragmentManager.beginTransaction().replace(R.id.content_main,
                        DrawingFragment.newInstance(), "Drawing fragment").commit()
                }
                R.id.nav_download_picture -> {
                    Toast.makeText(this@MainActivity, "В разработке", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_something -> {
                    Toast.makeText(this@MainActivity, "В разработке", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_about_app -> {
                    Toast.makeText(this@MainActivity, "В разработке", Toast.LENGTH_SHORT).show()
                }
            }
            // Закрытие DrawerLayout
            drawer.closeDrawer(GravityCompat.START)
            true
        }
    }

    // Создание диалогового окна с выбором цвета
    @SuppressLint("SetTextI18n")
    private fun createColorPickerDialog() {
        // Создание сброрщика для View
        val colorPickerBuilder = ColorPickerDialog.Builder(this)
            .setTitle("Выберите цвет и яркость")
            .setPositiveButton("Ок") { dialogInterface, _ -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true) // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last slide bar and buttons.

        // Получаем сам view и устанавливаем цвет
        val colorPickerDialog = colorPickerBuilder.colorPickerView
        colorPickerDialog?.setInitialColor(mainViewModel.stripConfig.curBaseColor)

        // Устанавливаем слушатель нажатий на цветовой круг
        colorPickerDialog?.setColorListener(ColorEnvelopeListener { envelope, _ ->
            if(connectToMcuStatus.value == ConnectStatus.CONNECT && mainViewModel.stripConfig.state.value == State.ENABLE) {
                if ((prevColorPickerColor != envelope.color) && (System.currentTimeMillis() - curTime > 100)) {
                    curTime  = System.currentTimeMillis()
                    prevColorPickerColor = envelope.color
                    httpHandler.post("${McuCommand.SET_BASE_COLOR.command}?color=${envelope.hexCode}")
                }
            }
        })
        colorPickerBuilder.show()
    }

    /*###################### Наблюдатели за состояниями переменных ############################*/
    // Наблюдатель за состоянием статуса подключения
    private fun observeConnectToMcuStatus(){
        connectToMcuStatus.observe(this) {
            when(it){
                ConnectStatus.NOT_CONNECT -> {
                    Toast.makeText(this, "Проверьте подключение к WiFi !", Toast.LENGTH_SHORT).show()

                    when(HttpHandler.lastCommand) {
                        // Команда по установке состояния ленты не выполнена!
                        McuCommand.SET_LED_STRIP_STATE.command -> mainViewModel.stripConfig.toggleStripState()

                        // Команда по установке состояния авто-смены эффектов не выполнена!
                        McuCommand.SET_AUTO_CHANGE_EFFECT_MODE.command -> mainViewModel.stripConfig.toggleAutoModeState()
                    }
                }

                ConnectStatus.RECEIVE_REQUEST -> {
                    // Ответ пришёл в следующем виде: "command: config1=value1;config2=value1;..."
                    // Получаем команду, на которую дан ответ и тело самого ответа
                    val requestCommand = HttpHandler.requestMessage.substringBefore(": ", "")
                    val requestBody = HttpHandler.requestMessage.substringAfter(": ", "")

                    Log.d("MY_LOGS", "New Command! RequestMessage: ${HttpHandler.requestMessage} \n$requestCommand :: $requestBody\n ")

                    when(requestCommand) {

                        // Установка настроек ленты
                        McuCommand.GET_ALL_CONFIG.command -> {
                            for(oneConfig in requestBody.split(";")) {

                                // Имя текущей настроки и её значение
                                val configName = oneConfig.substringBefore("=", "")
                                val configValue = try {
                                    oneConfig.substringAfter("=", "").toInt( if(configName == "curBaseColor") 16 else 10 )
                                    } catch (e: NumberFormatException) {
                                        addToLogList(LogTypes.ERROR,
                                            "Exception: NumberFormatException with ESP answer: ${HttpHandler.requestMessage}")
                                        -1
                                    }

                                // Установка статусов
                                when(configName) {
                                    "ledStripState" -> mainViewModel.stripConfig.state.value = if(configValue == 1) State.ENABLE else State.DISABLE
                                    "autoModeState" -> mainViewModel.stripConfig.autoModeState.value = if(configValue == 1) State.ENABLE else State.DISABLE
                                    "effectDelaySec" -> mainViewModel.stripConfig.oneEffectDelaySec.value = configValue
                                    "curEffectIndex" -> mainViewModel.stripConfig.curEffectIndex.value = configValue
                                    "curBaseColor" -> mainViewModel.stripConfig.curBaseColor = configValue
                                    "" -> {}
                                    else -> connectToMcuStatus.postValue(ConnectStatus.ERROR_IN_COMMAND)
                                }
                            }

                        }
                        McuCommand.SET_LED_STRIP_STATE.command -> {
                            val configValue = try {
                                requestBody.split(";")[0].substringAfter("=", "").toInt()
                            } catch (e: NumberFormatException) {
                                addToLogList(LogTypes.ERROR,
                                    "Exception: NumberFormatException with ESP answer: ${HttpHandler.requestMessage}")
                                -1
                            }
                            mainViewModel.stripConfig.state.value = if(configValue == 1) State.ENABLE else State.DISABLE
                        }
                        else -> connectToMcuStatus.postValue(ConnectStatus.ERROR_IN_COMMAND)
                    }

                    connectToMcuStatus.value = ConnectStatus.CONNECT
                    Log.d("MY_LOGS", "ledStripConfig: ${mainViewModel.stripConfig} ")
                    Log.d("MY_LOGS", "Mcu status after receive command: ${connectToMcuStatus.value} ")
                }
                else -> {}
            }
        }
    }

    /*==================== Вспомогательные функции =========================*/
    //---Автоматическое обновление приложения ------
    // Проверка наличия обновления приложения
    private fun checkForUpdate(){
        Thread {
            try {
                val url = URL("https://github.com/EmptyLibra/SmartGarlandApp_Esp8266/releases")
                val con1: URLConnection = url.openConnection()
                val htmlString = BufferedReader(InputStreamReader(con1.getInputStream())).readText()

                val matchRes = Regex("""/releases/tag/v.*"""").find(htmlString)

                if(matchRes != null) {
                    val newVersion = matchRes.groups[0].toString().substringAfter("/v").substringBefore("\"")

                    this@MainActivity.runOnUiThread {
                        if(newVersion > BuildConfig.VERSION_NAME) {
                            addToLogList(LogTypes.NOTE,
                                "Обновляюсь до новой версии:<br>$newVersion (Текущая версия: ${BuildConfig.VERSION_NAME})")
                            createDialogAskToUpdate(newVersion)

                        } else {

                            addToLogList(LogTypes.NOTE,
                                "У вас установлена последняя версия: ${BuildConfig.VERSION_NAME}")
                        }
                    }
                } else {
                    addToLogList(LogTypes.ERROR,
                        "Ошибка в поиске новой версии на сайте:<br>https://github.com/${url.path}<br><br>" +
                                "Сделайте скриншот и отправьте его разработчику!!!")
                }
            } catch (e: Exception) {
                addToLogList(LogTypes.ERROR,
                    "При открытии ссылки для проверки наличия обновления, возникло следующее исключение:<br>${e.message}<br><br>" +
                            "Сделайте скриншот и отправьте его разработчику!!!")
            }
        }.start()
    }

    // Создание диалога с предложением обновить приложения
    private fun createDialogAskToUpdate(newVersion: String){
        // Настройка текста основного сообщения
        val textView = TextView(this@MainActivity)
        textView.text = "Доступна новая версия приложения!"
        textView.gravity = Gravity.CENTER
        textView.textSize = 20.0F

        val message ="Новая версия: v$newVersion\n" +
                "Текущая версия: ${BuildConfig.VERSION_NAME}\n" +
                "Хотите обновить приложение?"

        // Создание диалогового окна с двумя кнопками
        val dialog = AlertDialog.Builder(this@MainActivity)
            .setCustomTitle(textView)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Да") { dialog, _ ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data =
                    Uri.parse("https://github.com/EmptyLibra/SmartGarlandApp_Esp8266/releases/download/v${newVersion}/smart-garland-v${newVersion}.apk")
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Позже") { dialog, _ ->
                dialog.cancel()
            }
            .create()
        dialog.show()

        // Смещение кнопок к центру
        val parent = dialog.getButton(AlertDialog.BUTTON_NEGATIVE).parent as LinearLayout
        parent.gravity = Gravity.CENTER_HORIZONTAL
        val leftSpacer = parent.getChildAt(1)
        leftSpacer.visibility = View.GONE
    }
}

