package com.ahs.adb.utils

import java.io.*


/**
 * @Description
 * @Author luxiao418
 * @Email luxiao418@pingan.com.cn
 * @Date 2020/10/26 3:00 下午
 * @Version
 */
object FileUtil {

    fun string2File(content: String, path: String) {
        var bufferedReader: BufferedReader? = null
        var bufferedWriter: BufferedWriter? = null
        val distFile = File(path)
        try {
            if (!distFile.parentFile.exists()) distFile.parentFile.mkdirs()
            bufferedReader = BufferedReader(StringReader(content))
            bufferedWriter = BufferedWriter(FileWriter(distFile))
            val buf = CharArray(1024) //字符缓冲区
            var len: Int
            while (bufferedReader.read(buf).also { len = it } != -1) {
                bufferedWriter.write(buf, 0, len)
            }
            bufferedWriter.flush()
        } catch (e: Exception) {
            println(e.toString())
        } finally {
            closeQuietly(bufferedReader)
            closeQuietly(bufferedWriter)
        }
    }

    private fun saveDataToFile(fileName: String, data: String) {
        var writer: BufferedWriter? = null
        val file = File("d:\\$fileName.json")
        //如果文件不存在，则新建一个
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        //写入
        try {
            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file, false), "UTF-8"))
            writer.write(data)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        println("文件写入成功！")
    }

    fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}