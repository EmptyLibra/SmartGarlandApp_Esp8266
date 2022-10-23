package com.master.esp8266_addressledscontroller.nav_fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.master.esp8266_addressledscontroller.R
import com.master.esp8266_addressledscontroller.databinding.EffectItemBinding

// Класс-модель для одного эффекта (хранит данные эффекта
data class Effect(val imageId: Int, val name: String, val index: Int, val standardDelay: Int)

// Адаптер, заполняющий список
class EffectsListAdapter(private val listener: Listener): RecyclerView.Adapter<EffectsListAdapter.ListHolder>() {
    private val effectList: Array<Effect> = arrayOf(
        Effect(android.R.drawable.btn_star_big_on, "Один цвет",       0,  0),
        Effect(android.R.drawable.btn_star_big_on, "Радуга",          1,  40),
        Effect(android.R.drawable.btn_star_big_on, "Конфети",         2,  20),
        Effect(android.R.drawable.btn_star_big_on, "Заполнитель",     3,  10),
        Effect(android.R.drawable.btn_star_big_on, "Блуждающий кубик",4,  130),
        Effect(android.R.drawable.btn_star_big_on, "Снег",            5,  140),
        Effect(android.R.drawable.btn_star_big_on, "Матрица",         6,  140),
        Effect(android.R.drawable.btn_star_big_on, "Падающие звёзды", 7,  140),
        Effect(android.R.drawable.btn_star_big_on, "Эффект9",         8,  10),
        Effect(android.R.drawable.btn_star_big_on, "Эффект10",        9,  10),
        Effect(android.R.drawable.btn_star_big_on, "Эффект11",        10, 10),
        Effect(android.R.drawable.btn_star_big_on, "Эффект12",        11, 10),
        Effect(android.R.drawable.btn_star_big_on, "Эффект13",        12, 10),
        Effect(android.R.drawable.btn_star_big_on, "Эффект14",        13, 10),
    )
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