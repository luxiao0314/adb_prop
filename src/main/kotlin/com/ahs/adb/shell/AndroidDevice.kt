package com.ahs.adb.shell

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

class AndroidDevice {

    val brand: String? by lazy {
        val brandResult = queryBrand()
        brandResult.data
    }

    val manufacturer: String? by lazy {
        val result = queryManufacturer()
        result.data
    }

    val osVersion: String? by lazy {
        val result = queryOSVersion()
        result.data
    }

    val model: String? by lazy {
        val result = queryModel()
        //result.data
        getFixedModel(result.data)
    }

    val name: String? by lazy {
        val result = queryName()
        result.data
    }

    val device: String? by lazy {
        val result = queryDevice()
        result.data
    }

    val storage: String? by lazy {
        val result = queryStorage()
        result.data
    }

    val memory: String? by lazy {
        val result = queryMemory()
        result.data
    }

    val cameraNum: String? by lazy {
        val result = queryCameraNum()
        result.data
    }

    val macAddr: String? by lazy {
        var result = queryMacAddr()
        //取值为空,或者为**,换命令再取一次
        if (result.data.isNullOrEmpty() || result.data.toString().contains("**")) {
            val macAddress = queryMacAddress()
            if (isValidMac(macAddress.data)) {
                result = macAddress
            }
        }
        result.data
    }

    val imei: String? by lazy {
        //部分andorid9的oppo手机,5取到的是meid,4才是imei
        checkImei(5) { checkImei(4) { checkImei(3) { checkImei(1) { checkImei(-2) { checkImei { "" } } } } } }
    }

    val serialNo: String? by lazy {
        val result = querySerialNumber()
        result.data
    }

    val phoneFileNum: Int? by lazy {
        queryPhoneFileNum()
    }

    private fun getFixedModel(queriedModel: String?): String? {
        var model: String? = queriedModel
        if (isXiaoMi()) {
            if (queriedModel?.toBigIntegerOrNull() != null) {
                model = device
            }
        } else if (isSamsung()) {
            val isSpecialSamSung: Boolean = queriedModel!!.startsWith("SM-G930", true)
                    || queriedModel.startsWith("SM-G935", true)
                    || queriedModel.startsWith("SM-G950", true)
                    || queriedModel.startsWith("SM-G955", true)
                    || queriedModel.startsWith("SM-N920", true)
                    || queriedModel.startsWith("SM-N950", true)
            if (isSpecialSamSung) {
                model = queryBaseBand().data?.let {
                    if (it.length > 4) {
                        "SM-${it.substring(0, 5)}"
                    } else {
                        model
                    }
                }
            }
        }

        return model
    }

    private fun queryBrand(): CmdResult<String?> {
        return Cmd.exec(ADB_COMMAND + "shell getprop ro.product.brand")
    }

    private fun queryManufacturer(): CmdResult<String?> {
        return Cmd.exec(ADB_COMMAND + "shell getprop ro.product.manufacturer")
    }

    private fun queryOSVersion(): CmdResult<String?> {
        return Cmd.exec(ADB_COMMAND + "shell getprop ro.build.version.release")
    }

    private fun queryModel(): CmdResult<String?> {
        return Cmd.exec(ADB_COMMAND + "shell getprop ro.product.model")
    }

    private fun queryName(): CmdResult<String?> {
        return Cmd.exec(ADB_COMMAND + "shell getprop ro.product.name")
    }

    private fun queryDevice(): CmdResult<String?> {
        return Cmd.exec(ADB_COMMAND + "shell getprop ro.product.device")
    }

    private fun queryBaseBand(): CmdResult<String?> {
        return Cmd.exec(ADB_COMMAND + "shell getprop gsm.version.baseband")
    }

    private fun queryStorage(): CmdResult<String?> {
        val storageResult = Cmd.exec(ADB_COMMAND + "shell df -h /data")
        if (!storageResult.data.isNullOrEmpty()) {
            storageResult.data = extractStorageInfo(storageResult.data!!).toInt().toString()
        }
        return storageResult
    }

    private fun queryMemory(): CmdResult<String?> {
        val memoryResult = Cmd.exec(ADB_COMMAND + """shell cat /proc/meminfo | grep MemTotal""")
        if (!memoryResult.data.isNullOrEmpty()) {
            memoryResult.data = extractMemory(memoryResult.data!!).toInt().toString()
        }
        return memoryResult
    }

    private fun queryCameraNum(): CmdResult<String?> {
        val cameraNumResult =
            Cmd.exec(ADB_COMMAND + """shell dumpsys media.camera | grep "Number of camera devices"""")
        if (!cameraNumResult.data.isNullOrEmpty()) {
            cameraNumResult.data = extractCameraNum(cameraNumResult.data!!).toString()
        }
        return cameraNumResult
    }

    private fun queryMacAddr(): CmdResult<String?> {
        val macAddrInfo = Cmd.exec(ADB_COMMAND + """shell ip address show wlan0 | grep link/ether""")
        if (!macAddrInfo.data.isNullOrEmpty()) {
            macAddrInfo.data = extractMacAddr(macAddrInfo.data!!)
        }
        println("queryMacAddr $macAddrInfo")
        return macAddrInfo
    }

    private fun queryMacAddress(): CmdResult<String?> {
        //adb shell cat /sys/class/net/wlan0/address
        val macAddrInfo = Cmd.exec(ADB_COMMAND + """shell cat /sys/class/net/wlan0/address""")
        println("queryMacAddress $macAddrInfo")
        return macAddrInfo
    }

    private fun checkImei(index: Int = -1, func: () -> String): String {
        val subInfoOneResult = when (index) {
            -1 -> queryImei()
            -2 -> queryPropImei()
            else -> queryImei(index)
        }
        return if (subInfoOneResult.isSuccess()) {
            subInfoOneResult.data?.let {
                if (it.isCorrectImei()) {
                    it
                } else {
                    func()
                }
            } ?: func()
        } else {
            func()
        }
    }

    /**
     * (未验证)
     *  Android 4.4 及以下版本可通过如下命令获取 IMEI：
     *  adb shell dumpsys iphonesubinfo
     *  Android 5.0 及以上版本
     *  adb shell service call iphonesubinfo 5 | awk -F "'" '{print $2}' | sed '1 d' | tr -d '.' | awk '{print}' ORS=
     */
    private fun queryImei(subInfoIndex: Int): CmdResult<String?> {
        val imeiResult = Cmd.exec(ADB_COMMAND + """shell service call iphonesubinfo $subInfoIndex""")
        imeiResult.data?.let {
            val imei = formatImeiData(it)
            if (imei.contains("Requires READ_PHONE_STATE")) {
                imeiResult.code = -1
                imeiResult.data = null
                imeiResult.message = imei
            } else {
                imeiResult.data = imei
            }
        }
//        println("queryImei $imeiResult")
        return imeiResult
    }

    private fun queryImei(): CmdResult<String?> {
        val imeiResult = Cmd.exec(ADB_COMMAND + "shell dumpsys iphonesubinfo")
        imeiResult.data?.let {
            val imei = formatImei(it)
            if (imei.contains("Requires READ_PHONE_STATE")) {
                imeiResult.code = -1
                imeiResult.data = null
                imeiResult.message = imei
            } else {
                imeiResult.data = imei
            }
        }
        println("queryImei $imeiResult")
        return imeiResult
    }

    //vivo x6 plus
    private fun queryPropImei(): CmdResult<String?> {
        val result = Cmd.exec(ADB_COMMAND + "shell getprop persist.sys.updater.imei")
        println("queryPropImei $result")
        return result
    }

    private fun querySerialNumber(): CmdResult<String?> {
        var retryCount = 0
        while (retryCount <= 2) {
            val result = if (brand == "samsung") {
                Cmd.exec(ADB_COMMAND + "shell getprop ril.serialnumber")
            } else {
                Cmd.exec(ADB_COMMAND + "shell getprop ro.serialno")
            }
//            println("querySerialNumber: $result")
            result.data?.let {
                if (!it.contains("daemon not running")) {
                    return result
                }
                retryCount++
            }
        }
        return CmdResult()
    }

    private fun extractStorageInfo(storageInfo: String): Double {
        val storageInfoList = storageInfo.split("\n").toMutableList()
        if (storageInfoList.count() >= 2) {
            val iterator = storageInfoList.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().contains("No such file or directory")) {
                    iterator.remove()
                }
            }
            val headerList = Regex("""\s+""").split(storageInfoList[0])
            val sizeIndex = headerList.indexOf("Size")
            val valList = Regex("""\s+""").split(storageInfoList[1])
            return guessExactStorageVal(Regex("""-?\d+""").find(valList[sizeIndex])!!.groupValues[0])
        }

        return -1.0
    }

    /**
     * 猜测手机厂商标注的磁盘存储大小.
     */
    private fun guessExactStorageVal(storageStr: String): Double {
        val storageVal = storageStr.toDouble()
        return 2.0.pow(ceil(log2(storageVal)))
    }

    private fun extractMemory(memoryInfo: String): Double {
        val memoryStr = Regex("""\s+""").split(Regex("""\s+""").split(memoryInfo)[1])[0]
        return guessExactMemoryVal(memoryStr)
    }

    private fun guessExactMemoryVal(memoryStr: String): Double {
        return ceil(memoryStr.toDouble() / 1024 / 1024)
    }

    private fun extractCameraNum(cameraInfo: String): Int {
        return Regex(""":\s+""").split(cameraInfo)[1].toInt()
    }

    private fun extractMacAddr(macAddrInfo: String): String {
        return macAddrInfo.split(" ")[1]
    }

    private fun formatImeiData(imeiInfo: String): String {
        var imeiStr = ""
        Regex("""\s'(.+)'""").findAll(imeiInfo).forEach { imeiStr += it.groups[1]!!.value }
        imeiStr = imeiStr.trim().replace(".", "")
            .replace("....", "")
        imeiStr = imeiStr.replace(Regex("""(.)([.])""")) {
            it.groups[1]!!.value
        }
        return imeiStr
    }

    private fun formatImei(imeiInfo: String): String {
        var imei = ""
        try {
            for (s in imeiInfo.split("=")) {
                if (isNumericZidai(s.trim())) {
                    imei = s.trim()
                    break
                }
            }
        } catch (e: Exception) {
        }
        return imei
    }

    private fun queryPhoneFileNum(): Int? {
        var num = 0

        num += queryPhoneFileNumByDir("/sdcard/DCIM/Camera")
        num += queryPhoneFileNumByDir("/sdcard/DCIM")
        num += queryPhoneFileNumByDir("/sdcard/IMG")
        num += queryPhoneFileNumByDir("/sdcard/相机")

        return num
    }

    private fun queryPhoneFileNumByDir(dir: String): Int {
        val queryResString = Cmd.exec(ADB_COMMAND + """shell ls -l """ + dir + """ | grep "^-" | wc -l""")
        return if (!queryResString.data?.trim().isNullOrEmpty()) queryResString.data?.trim()!!.toInt() else 0
    }

    private fun isXiaoMi() = "XIAOMI".isBrand()

    private fun isSamsung() = "SAMSUNG".isBrand()


    private fun String.isBrand() = brand.equals(this, true)

    enum class DeviceConnectStatus {
        NoDevice,
        Unauthorized,
        Connected
    }

    companion object {
        var ADB_COMMAND = ""

        private suspend fun restartAdbServer() {
            Cmd.exec(ADB_COMMAND + "kill-server")
            Cmd.exec(ADB_COMMAND + "start-server")
        }

        private fun queryAndroidDevice(): DeviceConnectStatus {
            var status = DeviceConnectStatus.NoDevice
            val queryDeviceResult = Cmd.exec(ADB_COMMAND + "devices")
            println("android device list: " + queryDeviceResult.data)
            val splitList = queryDeviceResult.data?.split("\n")
            if (splitList != null && splitList.count() > 1) {
                val deviceInfo = splitList[1]
                val deviceNameSplitList = Regex("""\s+""").split(deviceInfo)
                val deviceName = deviceNameSplitList.takeIf { it.count() == 2 }?.get(1)
                if (deviceName == "unauthorized") {
                    status = DeviceConnectStatus.Unauthorized
                } else if (!deviceName.isNullOrEmpty()) {
                    status = DeviceConnectStatus.Connected
                }
            }

            return status
        }

        fun showPhoneScreen() = Cmd.exec(ADB_COMMAND + "shell am start -a android.intent.action.MAIN -c android.intent.category.HOME")

        // 防止息屏
        fun screenOffTimeout() = Cmd.exec(ADB_COMMAND + "shell settings put system screen_off_timeout 2147483647")

        fun setScreenBrightnessMax() {
            setScreenBrightness(255)
        }

        fun setScreenBrightness(brightness: Int) {
            Cmd.exec(ADB_COMMAND + "shell settings put system screen_brightness_mode 0")
            Cmd.exec(ADB_COMMAND + "shell settings put system screen_brightness $brightness")
        }

        private fun filterData(querySimCardResult: CmdResult<String?>): String? {
            val data = querySimCardResult.data
            return if (data != null) {
                data.replace(",", "").replace("Unknown", "").replace("无服务", "").replace("仅限紧急呼叫", "").trim()
            } else {
                ""
            }
        }

        fun screenBrightness() {
            Cmd.exec(ADB_COMMAND + "shell settings put system screen_brightness_mode 1")
        }

        fun enableWifiDebug() {
            val result = Cmd.exec(ADB_COMMAND + "tcpip 5555")
            println("无线调试: tcpip $result")
        }

        fun connectADBServer(ip: String?): String {
            val result = Cmd.exec(ADB_COMMAND + "connect $ip:5555")
            println("无线调试: connect $result ip: $ip")
            return result.data.toString()
        }

        fun disconnectADBServer(ip: String?) {
            val result = Cmd.exec(ADB_COMMAND + "disconnect $ip:5555")
            println("无线调试: disconnect $result ip: $ip")
        }

        /**
         * 关闭所有连接
         */
        fun disconnectADBServer() {
            val result = Cmd.exec(ADB_COMMAND + "disconnect")
            println("无线调试: disconnect all $result")
        }

        /**
         * 获取设备列表
         */
        fun devices(): String {
            val result = Cmd.exec(ADB_COMMAND + "devices")
            println("无线调试: devices $result")
            return result.data.toString()
        }

        fun lightUpScreen(): CmdResult<String?> {
            val result = Cmd.exec(ADB_COMMAND + "shell input keyevent 224")
            println("无线调试: keyevent $result")
            return result
        }
    }

    /**
     * 判断IMEI号是否合法，合法返回true，不合法返回false
     *
     * @param imei IMEI号
     * @return if 合法 true else false
     */
    private fun String.isCorrectImei(): Boolean {
        var imei = this.trim()
        val imeiLength = 15
        if (imei.length == imeiLength) {
            val check = Integer.valueOf(imei.substring(14))
            imei = imei.substring(0, 14)
            val imeiChar = imei.toCharArray()
            var resultInt = 0
            var i = 0
            while (i < imeiChar.size) {
                val a = Integer.parseInt(imeiChar[i].toString())
                i++
                val temp = Integer.parseInt(imeiChar[i].toString()) * 2
                val b = if (temp < 10) temp else temp - 9
                resultInt += a + b
                i++
            }
            resultInt %= 10
            resultInt = if (resultInt == 0) 0 else 10 - resultInt
            if (resultInt == check) {
                return true
            }
        }
        return false
    }

    /*
     * 是否为数字
     */
    private fun isNumericZidai(str: String): Boolean {
        for (i in str.indices) {
            println(str[i])
            if (!Character.isDigit(str[i])) {
                return false
            }
        }
        return true
    }

    private fun isValidMac(macStr: String?): Boolean {
        val macAddressRule = "([A-Fa-f0-9]{2}[-,:]){5}[A-Fa-f0-9]{2}"
        // 这是真正的MAC地址；正则表达式；
        return macStr?.matches(macAddressRule.toRegex()) ?: false
    }
}

class Photo(var time: String = "", var path: String = "")
