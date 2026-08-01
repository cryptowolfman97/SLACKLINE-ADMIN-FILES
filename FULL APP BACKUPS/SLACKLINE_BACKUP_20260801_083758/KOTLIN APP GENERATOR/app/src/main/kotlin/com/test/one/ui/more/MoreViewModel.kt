package com.test.one.ui.more

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MoreViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "More"
    }
    val text: LiveData<String> = _text
}