package com.master.esp8266_addressledscontroller.side_nav_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.master.esp8266_addressledscontroller.databinding.FragmentDrawingBinding

class DrawingFragment : Fragment() {
    private lateinit var binding: FragmentDrawingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDrawingBinding.inflate(inflater, container, false)

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = DrawingFragment()
    }
}