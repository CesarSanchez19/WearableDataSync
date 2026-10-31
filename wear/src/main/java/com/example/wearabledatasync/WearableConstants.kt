package com.example.wearabledatasync

// The path the Watch will use to send a command to the Phone
const val MESSAGE_PATH_RESET = "/reset_counter"

// The path the Phone will use to send a confirmation back to the Watch
const val MESSAGE_PATH_CONFIRM = "/reset_confirm"

// Paths the Watch uses to ask the Phone to change its counter by COUNTER_STEP
const val MESSAGE_PATH_INCREMENT = "/increment_counter"
const val MESSAGE_PATH_DECREMENT = "/decrement_counter"

// Counter rules shared by both devices
const val COUNTER_MIN = 0
const val COUNTER_MAX = 100
const val COUNTER_STEP = 5

// Prefix of the confirmation payload that carries the new counter value (e.g. "COUNT_OK:95")
const val CONFIRM_COUNT_PREFIX = "COUNT_OK:"

// Text hint (not only color) when the counter reaches one of its limits
fun counterLimitLabel(value: Int): String = when (value) {
    COUNTER_MAX -> " (MAX)"
    COUNTER_MIN -> " (MIN)"
    else -> ""
}