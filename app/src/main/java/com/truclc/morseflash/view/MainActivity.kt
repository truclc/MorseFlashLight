package com.truclc.morseflash.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.support.wearable.view.BoxInsetLayout
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.truclc.morseflash.data.Message
import com.truclc.morseflash.util.FlashlightManager
import com.truclc.morseflash.util.MorseConverter
import com.truclc.morseflashlight.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : WearableActivity() {
    private lateinit var inputText: EditText
    private lateinit var convertButton: Button
    private lateinit var repeatSpinner: Spinner
    private lateinit var boxInsertLayout: BoxInsetLayout
    private var isSpinnerInitialized = false
    private lateinit var morseCodeTextView: TextView
    private var scope = CoroutineScope(Dispatchers.Default)
    private var isConverting = false // Variable to track the status of the transition
    private var repeatTimes = 0
    private var count = 0
    private var isInputValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        boxInsertLayout = findViewById(R.id.box_insert_layout)
        inputText = findViewById(R.id.input_text)
        convertButton = findViewById(R.id.convert_button)
        repeatSpinner = findViewById(R.id.repeat_spinner)
        morseCodeTextView = findViewById(R.id.morse_code_text)
        convertButton.isEnabled = false
        boxInsertLayout.setBackgroundColor(Color.WHITE)
        repeatSpinner.setBackgroundColor(Color.WHITE)
        inputText.setTextColor(Color.BLACK)

        inputTextListener()
        setupSpinner()
        setupButtonClick()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (scope.isActive) {
            scope.cancel()
        }
    }

    private fun isValidMorseCode(input: String): Boolean {
        // A valid Morse string will only contain the characters '.', '-', '/' or the space ' '
        val validChars = setOf('.', '-', ' ', '/')
        for (char in input) {
            if (char !in validChars) {
                return false
            }
        }
        return true
    }

    private fun isValidInput(input: String): Boolean {
        val validChars = setOf(
            ' ',
            '.',
            '-',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F',
            'G',
            'H',
            'I',
            'J',
            'K',
            'L',
            'M',
            'N',
            'O',
            'P',
            'Q',
            'R',
            'S',
            'T',
            'U',
            'V',
            'W',
            'X',
            'Y',
            'Z',
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9'
        )
        // Remove whitespace at the beginning and end of the string
        val trimmedInput = input.trim()

        // Replace the character "'" with a blank character
        val sanitizedInput = trimmedInput.replace("'", " ")

        val lowerCaseInput = sanitizedInput.toUpperCase()
        for (char in lowerCaseInput) {
            if (char !in validChars) {
                return false
            }
        }
        return true
    }

    private fun inputTextListener() {
        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isInputValid = s?.isNotEmpty() ?: false
                convertButton.isEnabled = isInputValid
            }

            override fun afterTextChanged(s: Editable?) {
                // After the text changes, update the TextView with the corresponding Morse code
                val input = s.toString()
                val message = Message(input)
                val morseCode = MorseConverter.textToMorse(message.text)
                morseCodeTextView.text = morseCode

            }
        })
    }

    private fun setupSpinner() {
        val repeatOptions = resources.getStringArray(R.array.repeat_options)
        val adapter = ArrayAdapter(this, R.layout.custom_spinner_item, repeatOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        repeatSpinner.adapter = adapter

        // Listen for event when user selects an item from Spinner
        repeatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @SuppressLint("StringFormatMatches")
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (isSpinnerInitialized) {
                    val selectedRepeatTimes = repeatOptions[position].toInt()
                    val toastMessage =
                        getString(R.string.notify_loop_times_text, selectedRepeatTimes)
                    showAlert(toastMessage)
                    isConverting = false
                } else {
                    // Marks that the Spinner has been initialized
                    isSpinnerInitialized = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing if no items are selected
            }
        }
    }

    private fun setupButtonClick() {
        convertButton.setOnClickListener {
            Log.i("TrucLC", "isConverting = $isConverting")
            if (isConverting) {
                isConverting = false
                count = 0
                convertButton.text = getString(R.string.convert_convert_text)
                boxInsertLayout.setBackgroundColor(Color.WHITE)
                repeatSpinner.setBackgroundColor(Color.WHITE)
                repeatSpinner.isEnabled = true
                inputText.isEnabled = true
                morseCodeTextView.apply {
                    setSingleLine(true)
                    isSelected = false
                }
                scope.cancel() // Cancel all running coroutines
                Log.i("TrucLC", "Conversion process stopped")
            } else {
                isConverting = true

                // Start the conversion process
                val input = inputText.text.toString()
                Log.i("TrucLC", "isValidInput = ${isValidInput(input)}")

                if (!isValidInput(input)) {
                    showAlert(getString(R.string.warning_input_string))
                    return@setOnClickListener
                }

                val message = Message(input)
                val morseCode = MorseConverter.textToMorse(message.text)
                morseCodeTextView.apply {
                    setSingleLine(true)
                    isSelected = true
                }

                // Check Morse code validity
                if (!isValidMorseCode(morseCode)) {
                    showAlert(getString(R.string.warning_morse_code))
                    return@setOnClickListener
                }

                // Display notifications and update button status
                convertButton.text = getString(R.string.convert_pause_text)
                repeatSpinner.isEnabled = false
                inputText.isEnabled = false

                repeatTimes =
                    repeatSpinner.selectedItem.toString().toInt() // Get the number of reruns from the Spinner
                Log.i("TrucLC", "repeatTimes = $repeatTimes")
                Log.i("TrucLC", "scope active = ${scope.isActive}")
                if (!scope.isActive) {
                    scope = CoroutineScope(Dispatchers.Default) // Initialize a new CoroutineScope
                }

                scope.launch {
                    repeat(repeatTimes) {
                        println("Loop times ${it + 1} start")
                        sendMorseCode(morseCode)
                        delay(2000)
                        println("Loop times ${it + 1} end")
                    }
                }
            }
        }
    }

    private fun showAlert(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("StringFormatMatches")
    private suspend fun sendMorseCode(morseCode: String) {
        scope.launch {
            for ((index, c) in morseCode.withIndex()) {

                runOnUiThread {
                    Log.i("TrucLC", "index = $index")
                    highlightMorseCode(index)
                }
                when (c) {
                    '.' -> {
                        runOnUiThread {
                            FlashlightManager.turnOnFlashlight(this@MainActivity)
                            boxInsertLayout.setBackgroundColor(Color.WHITE)
                            repeatSpinner.setBackgroundColor(Color.WHITE)

                            Log.i("TrucLC", "Duration of dot")
                        }
                        delay(100) // Duration of dot
                    }

                    '-' -> {
                        runOnUiThread {
                            FlashlightManager.turnOnFlashlight(this@MainActivity)
                            boxInsertLayout.setBackgroundColor(Color.WHITE)
                            repeatSpinner.setBackgroundColor(Color.WHITE)

                            Log.i("TrucLC", "Duration of dash")
                        }
                        delay(300) // Duration of dash
                    }

                    ' ' -> {
                        runOnUiThread {
                            FlashlightManager.turnOffFlashlight(this@MainActivity)
                            boxInsertLayout.setBackgroundColor(Color.BLACK)
                            repeatSpinner.setBackgroundColor(Color.BLACK)

                            Log.i("TrucLC", "Pause between letters 100MS")
                        }
                        delay(100) // Pause between letters
                    }
                }
                runOnUiThread {
                    FlashlightManager.turnOffFlashlight(this@MainActivity)
                    boxInsertLayout.setBackgroundColor(Color.BLACK)
                    repeatSpinner.setBackgroundColor(Color.BLACK)

                    Log.i("TrucLC", "Pause between words 300MS")
                }
                delay(300) // Pause between words
                Log.i("TrucLC", "count = $count")

                if (repeatTimes > 1) {
                    if (index == morseCode.lastIndex) {
                        count++
                        Log.i("TrucLC", "Play done $count times")
                        if (count == repeatTimes) {
                            runOnUiThread {
                                isConverting = false
                                inputText.text.clear()
                                morseCodeTextView.text = ""
                                convertButton.text = getString(R.string.convert_convert_text)
                                boxInsertLayout.setBackgroundColor(Color.WHITE)
                                repeatSpinner.setBackgroundColor(Color.WHITE)
                                repeatSpinner.isEnabled = true
                                inputText.isEnabled = true
                                showAlert(getString(R.string.notify_finished_flashing_times, count))
                                count = 0
                            }
                        }
                    }
                } else {
                    if (index == morseCode.lastIndex) {
                        runOnUiThread {
                            isConverting = false
                            inputText.text.clear()
                            morseCodeTextView.text = ""
                            convertButton.text = getString(R.string.convert_convert_text)
                            boxInsertLayout.setBackgroundColor(Color.WHITE)
                            repeatSpinner.setBackgroundColor(Color.WHITE)
                            repeatSpinner.isEnabled = true
                            inputText.isEnabled = true
                            showAlert(getString(R.string.notify_finished_flashing))
                            Log.i("TrucLC", "Only once done!")
                        }

                    }
                }
            }
        }.join()
    }

    private fun highlightMorseCode(index: Int) {
        val morseCode = morseCodeTextView.text.toString()
        val spannableString = SpannableString(morseCode)

        for (i in morseCode.indices) {
            if (i == index) {
                spannableString.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            applicationContext, android.R.color.holo_red_dark
                        )
                    ), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Larger font size (1.5 times the original font size)
                spannableString.setSpan(
                    RelativeSizeSpan(1.5f), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        morseCodeTextView.text = spannableString
        morseCodeTextView.gravity = Gravity.CENTER_HORIZONTAL
    }
}