package com.master.esp8266_addressledscontroller

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import androidx.drawerlayout.widget.DrawerLayout

import android.R.id
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.master.esp8266_addressledscontroller.databinding.ActivityMainBinding
import com.master.esp8266_addressledscontroller.nav_fragments.DrawingFragment
import com.master.esp8266_addressledscontroller.nav_fragments.SettingsFragment


/* Для связи с МК по wi-fi надо:
* 1. Добавить в AndroidManifest разрешение на использование интернета и http.
* 2. Добавить в build.gradle (Module) библиотеку okhttp3
*
* Запуск отладки по wifi: StartWifiDebug.bat
*Почитать о:
*  Тестирование мобильных приложений.
*
* */

class MainActivity : AppCompatActivity() {
    private lateinit var  mainBinding: ActivityMainBinding  // Класс привязки к xml файлу

    // Функция создания активности (вызывается при её создании)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение контента из xml файла
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        // Куда поместить новый фрагмент
        supportFragmentManager.beginTransaction().replace(R.id.content_main,
            SettingsFragment.newInstance()).commit()

        // ToolBar
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_home)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Встроенная стрелка возвращения
        supportActionBar?.title = "Умная гирлянда"         // Название в ToolBar

        // Обработчик пунктов выдвижного меню
        mainBinding.apply {
            nvMenu.setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.nav_settings -> {
                        // Запуск нового фрагмента
                        supportFragmentManager.beginTransaction().replace(R.id.content_main,
                            SettingsFragment.newInstance()).commit()
                    }
                    R.id.nav_draw -> {
                        supportFragmentManager.beginTransaction().replace(R.id.content_main,
                            DrawingFragment.newInstance()).commit()
                    }
                    R.id.nav_download_picture -> {
                        Toast.makeText(this@MainActivity, "В разработке", Toast.LENGTH_SHORT).show()
                    }
                    R.id.nav_something -> {
                        Toast.makeText(this@MainActivity, "В разработке", Toast.LENGTH_SHORT).show()
                    }
                    R.id.nav_for_developer -> {
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
        Log.d("MY_LOGS", "Method: onCreate()")

    }

    // Обработка нажатий кнопок на ToolBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean = with(mainBinding) {
        when(item.itemId){
            android.R.id.home -> {
                if(drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START)
                } else {
                    drawer.openDrawer(GravityCompat.START)
                }
            }
        }
        return true
    }






}

