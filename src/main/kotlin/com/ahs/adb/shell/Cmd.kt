package com.ahs.adb.shell


object Cmd {

    fun exec(command: String): CmdResult<String?> {
        val result = CmdResult<String?>()
        val res = ShellKit.adb(command)
        if (res.isBlank()) {
            result.code = -1
        } else {
            result.code = 0
            result.data = res
        }
        return result
    }
}

class CmdResult<T> {
    var code: Int = -1
    var data: T? = null
    var message: String? = ""

    fun isSuccess(): Boolean = code == 0
}