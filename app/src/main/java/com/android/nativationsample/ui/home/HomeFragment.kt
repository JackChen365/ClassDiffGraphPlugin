package com.android.nativationsample.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.nativationsample.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    // Obtain ViewModel
    private val viewmodel: LiveDataViewModel by viewModels { LiveDataVMFactory }

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.refreshButton.setOnClickListener {
            viewmodel.onRefresh()
        }
        viewmodel.currentTime.observe(viewLifecycleOwner) { value ->
            binding.time.text = value?.toString()
        }
        viewmodel.currentTimeTransformed.observe(viewLifecycleOwner) { value ->
            binding.timeTransformed.text = value
        }
        viewmodel.currentWeather.observe(viewLifecycleOwner) { value ->
            binding.currentWeather.text = value
        }
        viewmodel.cachedValue.observe(viewLifecycleOwner) { value ->
            binding.cachedValue.text = value
        }
    }
}