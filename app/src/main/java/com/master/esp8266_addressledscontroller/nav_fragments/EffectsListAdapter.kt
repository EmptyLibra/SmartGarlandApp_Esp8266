package com.master.esp8266_addressledscontroller.nav_fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.master.esp8266_addressledscontroller.R
import com.master.esp8266_addressledscontroller.databinding.EffectItemBinding

// Класс-модель для одного эффекта (хранит данные эффекта
data class Effect(val imageId: Int, val displayName: String, val systemName: String, val index: Int, val standardDelay: Int)

// Адаптер, заполняющий список
class EffectsListAdapter(private val listener: Listener): RecyclerView.Adapter<EffectsListAdapter.ListHolder>() {
    private val effectList: Array<Effect> = arrayOf(
        Effect(android.R.drawable.btn_star_big_on, "Один цвет",       "Empty effect",      0,  0),
        Effect(android.R.drawable.btn_star_big_on, "Радуга",          "Rainbow",           1,  40),
        Effect(android.R.drawable.btn_star_big_on, "Конфети",         "Confetti",          2,  20),
        Effect(android.R.drawable.btn_star_big_on, "Заполнитель",     "Cyclic filler",     3,  10),
        Effect(android.R.drawable.btn_star_big_on, "Блуждающий кубик","Wandering ball",    4,  130),
        Effect(android.R.drawable.btn_star_big_on, "Снег",            "Falling snow",      5,  140),
        Effect(android.R.drawable.btn_star_big_on, "Матрица",         "Matrix",            6,  140),
        Effect(android.R.drawable.btn_star_big_on, "Падающие звёзды", "Falling stars",     7,  140),
        Effect(android.R.drawable.btn_star_big_on, "Камин",           "Fireplace",         8,  110),
        Effect(android.R.drawable.btn_star_big_on, "Мячики",          "Bouncing Balls",    9,  120),
        Effect(android.R.drawable.btn_star_big_on, "Цветные полосы",  "Fading color lines",10, 180),
        Effect(android.R.drawable.btn_star_big_on, "Эффект13",        "",                  11, 50),
        Effect(android.R.drawable.btn_star_big_on, "Эффект14",        "",                  12, 10),
    )

    // Контейнер для элемента списка, который хранит ссылку на свой view
    class ListHolder(item: View): RecyclerView.ViewHolder(item) {
        private val itemBinding = EffectItemBinding.bind(item) // Ссылка на главный элемент разметки списка

        fun bind(effect: Effect, listener: Listener) = with(itemBinding){
            effectIcon.setImageResource(effect.imageId)
            effectName.text = effect.displayName

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