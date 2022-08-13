package com.master.esp8266_addressledscontroller.nav_fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.master.esp8266_addressledscontroller.R
import com.master.esp8266_addressledscontroller.databinding.FragmentDrawingBinding
import com.master.esp8266_addressledscontroller.databinding.FragmentSettingsBinding

class DrawingFragment : Fragment() {
    private lateinit var binding: FragmentDrawingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDrawingBinding.inflate(inflater, container, false)

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = DrawingFragment()
    }
}