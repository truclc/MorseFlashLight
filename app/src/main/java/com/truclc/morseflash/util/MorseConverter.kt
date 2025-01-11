package com.truclc.morseflash.util

object MorseConverter {
    private val MORSE_CODE_ARRAY = arrayOf(
        ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", // A-I
        ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", // J-R
        "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", // S-Z
        "-----", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----." // 0-9
    )

    fun textToMorse(text: String): String {
        val upperCaseText = text.toUpperCase()
        val morseCode = StringBuilder()
        for (c in upperCaseText) {
            when {
                Character.isWhitespace(c) -> morseCode.append(" / ") // Add spaces between words
                Character.isLetter(c) -> {
                    val index = c - 'A'
                    morseCode.append("${MORSE_CODE_ARRAY[index]} ") // Add a space after each Morse character
                }
                Character.isDigit(c) -> {
                    val index = c - '0' + 26 // Start at index 26 in the Morse array to process numeric characters
                    morseCode.append("${MORSE_CODE_ARRAY[index]} ") // Add a space after each Morse character
                }
            }
        }
        return morseCode.toString()
    }
}