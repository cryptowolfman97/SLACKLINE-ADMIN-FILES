package com.test.one.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBindingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(MoreViewModel::class.java)
        _binding = FragmentMoreBindingBinding.inflate(inflater, container, false)
        viewModel.text.observe(viewLifecycleOwner) { binding.textContent.text = it }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}