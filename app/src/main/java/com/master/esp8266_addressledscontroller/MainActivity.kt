package com.master.esp8266_addressledscontroller

import android.R.id
import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import com.master.esp8266_addressledscontroller.databinding.ActivityMainBinding
import com.master.esp8266_addressledscontroller.nav_fragments.DrawingFragment
import com.master.esp8266_addressledscontroller.nav_fragments.EffectsFragment
import java.time.LocalTime


/* Для связи с МК по wi-fi надо:
* 1. Добавить в AndroidManifest разрешение на использование интернета и http.
* 2. Добавить в build.gradle (Module) библиотеку okhttp3
*
* Что сделать:
* 1. Добавить список эффектов с Recycle view
*
* Запуск отладки по wifi: .\StartWifiDebug.bat
*Почитать о:
*  Тестирование мобильных приложений.
*
* */

class MainActivity : AppCompatActivity() {
    private lateinit var  mainBinding: ActivityMainBinding  // Класс привязки к xml файлу
    private val dataModel: MainActivityViewModel by viewModels()

    // Функция создания активности (вызывается при её создании)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение контента из xml файла
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        // Инициализация списка с отладочной информацией (иначе первые две записи не добавляюся)
        dataModel.initLogList()

        // Куда поместить новый фрагмент
        if(savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(R.id.content_main, EffectsFragment.newInstance(), "Settings fragment").commit()
        }

        // ToolBar
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_home)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Встроенная стрелка возвращения
        supportActionBar?.title = "Умная гирлянда"         // Название в ToolBar

        // Обработчик пунктов выдвижного меню
        mainBinding.apply {
            nvMenu.setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.nav_settings -> {
                        supportActionBar?.title = "Умная гирлянда"

                        // Запуск нового фрагмента
                        supportFragmentManager.beginTransaction().replace(R.id.content_main,
                            EffectsFragment.newInstance(), "Settings fragment").commit()
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        menu.findItem(R.id.change_color).isVisible = supportFragmentManager.findFragmentByTag("Settings fragment") != null
        return true
    }

    // Обработка нажатий кнопок на ToolBar
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean = with(mainBinding) {
        when(item.itemId){
            id.home -> {
                if(drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START)
                } else {
                    drawer.openDrawer(GravityCompat.START)
                }
            }

            R.id.change_color -> {
                // Запуск окна с выбором цвета
                val settingsFrag = supportFragmentManager.findFragmentById(R.id.content_main)
                if(settingsFrag is EffectsFragment) // Пока что это костыль !!!!!!!!!!!!!!!!!
                    settingsFrag.createColorPickerDialog()
            }
            R.id.logs -> {
                val diaLogs = Dialog(this@MainActivity)
                diaLogs.setTitle("Логи")
                diaLogs.setContentView(R.layout.logs_layout)

                val textView = diaLogs.findViewById<TextView>(R.id.textViewLogs)
                textView.movementMethod = ScrollingMovementMethod()
                var text = ""
                for(elem in dataModel.logList.value!!) {
                    text += elem.toString()
                }
                textView.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                textView.setBackgroundColor(Color.BLACK)
                textView.gravity = Gravity.BOTTOM
                diaLogs.show()
            }
        }
        return true
    }
}

