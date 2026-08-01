package com.test.one.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.test.one.MainActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBindingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBindingBinding.inflate(inflater, container, false)
        viewModel.text.observe(viewLifecycleOwner) { binding.textContent.text = it }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}