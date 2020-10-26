package com.ahs.adb

import com.ahs.adb.shell.AndroidDevice
import com.ahs.adb.shell.ShellKit
import com.ahs.adb.utils.FileUtil
import java.io.File


/**
 * @Description
 * @Author luxiao418
 * @Email luxiao418@pingan.com.cn
 * @Date 2020/10/24 2:36 下午
 * @Version
 */
fun main(args: Array<String>) {

    var hasResult = false

    while (true) {

        val androidDevice = AndroidDevice()

        val brand = androidDevice.brand?.trim().toString()

        Thread.sleep(1000)

        if (!brand.contains("no devices") && !brand.contains("unauthorized")) {

            val name = "${brand.toUpperCase()}_${androidDevice.model?.trim()}_${androidDevice.imei}.txt"

            val path = "/Users/lux/Downloads/prop/$name"

            println("filename: $name")

            if (!hasResult || !File(path).exists()) {

                hasResult = true

                val result = ShellKit.adb("shell getprop")

                FileUtil.string2File(result, path)

                println("保存成功: $path")
            }
        } else {
            hasResult = false
            println(brand)
        }
    }
}