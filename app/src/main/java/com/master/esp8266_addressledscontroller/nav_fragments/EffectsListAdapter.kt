package com.master.esp8266_addressledscontroller.nav_fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.master.esp8266_addressledscontroller.R
import com.master.esp8266_addressledscontroller.databinding.EffectItemBinding


// Класс-модель для одного эффекта (хранит данные эффекта
data class Effect(val imageId: Int, val name: String, val index: Int)

// Адаптер, заполняющий список
class EffectsListAdapter(private val listener: Listener): RecyclerView.Adapter<EffectsListAdapter.ListHolder>() {
    private val effectsNameArr = arrayOf("Один цвет", "Радуга", "Конфети", "Заполнение")
    private val effectList: Array<Effect> = Array(effectsNameArr.size) { i -> Effect(android.R.drawable.btn_star_big_on, effectsNameArr[i], i)}

    // Контейнер для элемента списка, который хранит ссылку на свой view
    class ListHolder(item: View): RecyclerView.ViewHolder(item) {
        private val itemBinding = EffectItemBinding.bind(item) // Ссылка на главный элемент разметки списка

        fun bind(effect: Effect, listener: Listener) = with(itemBinding){
            effectIcon.setImageResource(effect.imageId)
            effectName.text = effect.name

            itemView.setOnClickListener{
                listener.effectListOnItemClick(effect)
            }
        }
    }

    // Интерфейс со слушателем нажатий для элемента списка
    interface Listener{
        fun effectListOnItemClick(effect: Effect)
    }

    // Создаёт из разметки элемента готовый элемент, который можно будет заполнять
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
        // Получаем View из разметки
        val view = LayoutInflater.from(parent.context).inflate(R.layout.effect_item, parent, false)
        return ListHolder(view)
    }

    // Заполняет элемент списка конкретными картинками, текстом...
    override fun onBindViewHolder(holder: ListHolder, position: Int) {
        holder.bind(effectList[position], listener)
    }

    // Количество элементов в списке
    override fun getItemCount(): Int {
        return effectList.size
    }
}