package com.ahs.adb

import com.ahs.adb.shell.AndroidDevice
import com.ahs.adb.shell.ShellKit
import com.ahs.adb.utils.FileUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


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

        val brand = androidDevice.brand?.trim().toString().toUpperCase()

        Thread.sleep(1000)

        if (!brand.contains("NO DEVICES") && !brand.contains("UNAUTHORIZED") && !brand.contains("ERROR")) {

            val time = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(Date())

            val name = "${brand}/${time}_${androidDevice.model?.trim()}(${brand})_${androidDevice.imei}"

            println(name)

            if (!hasResult) {

                val path = "/Users/lux/Downloads/prop/$name.txt"

                println("filename: $name")

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