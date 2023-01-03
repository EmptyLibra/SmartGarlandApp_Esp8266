package com.master.esp8266_addressledscontroller.main_tab_fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.master.esp8266_addressledscontroller.HttpHandler
import com.master.esp8266_addressledscontroller.MainActivityViewModel
import com.master.esp8266_addressledscontroller.McuCommand
import com.master.esp8266_addressledscontroller.State
import com.master.esp8266_addressledscontroller.databinding.FragmentEffectsBinding


class EffectsFragment : Fragment(), EffectsListAdapter.Listener {
    // Привязка к Layout фрагмента
    private lateinit var effectFragmentBinding: FragmentEffectsBinding

    // ViewModel главной активности
    private val mainViewModel: MainActivityViewModel by activityViewModels()

    private val httpHandler = HttpHandler.newInstance()

    companion object{
        // Экземпляр фрагмента создастся только один раз, а дальше будет браться уже существующий
        @JvmStatic
        fun newInstance() = EffectsFragment()
    }

    /*###################### Методы цикла жизни фрагмента ############################*/
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Привязка Layout к фрагменту
        effectFragmentBinding = FragmentEffectsBinding.inflate(inflater, container, false)

        //----------- Обработчики нажатий -----------
        effectFragmentBinding.apply {
            // Настраиваем список эффектов
            effectsList.layoutManager = GridLayoutManager(this@EffectsFragment.context, 2)
            effectsList.adapter = EffectsListAdapter(this@EffectsFragment) // Адаптер для списка с эффектами
        }
        return effectFragmentBinding.root
    }

/*    // Сохранение данных
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putString("asd", "asd")
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val text = savedInstanceState?.get("asd")
    }*/

    /*###################### Методы обработки нажатий ############################*/
    // Обработчик нажатий на элементы списка с эффектами
    override fun effectListOnItemClick(effect: Effect) =with(effectFragmentBinding) {
        mainViewModel.stripConfig.curEffectIndex.value = effect.index
        if(mainViewModel.stripConfig.state.value == State.ENABLE) {
            //post("cmd?getEffectDelayMs=${effect.systemName}") // Получаем задержку эффекта, устанавливаем её в slider
            httpHandler.post("${McuCommand.CHANGE_EFFECT.command}?ef=mode${effect.index}")              // Запуск эффекта

        }  else {
            Toast.makeText(this@EffectsFragment.context, "Лента выключена!", Toast.LENGTH_SHORT).show()
        }
    }
}