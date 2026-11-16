package com.awakedrive.drowsedetect.service

/** MainActivity đang hiển thị hay không (đặt ở onStart/onStop). Service dùng để chọn overlay hay màn trong app. */
object AppForeground {
    @Volatile var visible: Boolean = false
}
