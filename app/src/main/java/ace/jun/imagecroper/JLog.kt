package ace.jun.imagecroper

object JLog {
    fun logD(message: String?) {
//        if (BuildConfig.DEBUG) {
        val fullClassName =
            Thread.currentThread().stackTrace[3].className
        val className =
            fullClassName.substring(fullClassName.lastIndexOf(".") + 1)
        val methodName =
            Thread.currentThread().stackTrace[3].methodName
        val lineNumber =
            Thread.currentThread().stackTrace[3].lineNumber
        android.util.Log.d(
            "[$className], [$methodName], [$lineNumber] ",
            "\n${message ?: "null"}"
        )
//        }
    }

    fun logI(message: String?) {
//        if (BuildConfig.DEBUG) {
        val fullClassName =
            Thread.currentThread().stackTrace[3].className
        val className =
            fullClassName.substring(fullClassName.lastIndexOf(".") + 1)
        val methodName =
            Thread.currentThread().stackTrace[3].methodName
        val lineNumber =
            Thread.currentThread().stackTrace[3].lineNumber
        android.util.Log.i(
            "[$className], [$methodName], [$lineNumber] ",
            "\n${message ?: "null"}"
        )
    }

fun logE(message: String?) {
//        if (BuildConfig.DEBUG) {
    val fullClassName =
        Thread.currentThread().stackTrace[3].className
    val className =
        fullClassName.substring(fullClassName.lastIndexOf(".") + 1)
    val methodName =
        Thread.currentThread().stackTrace[3].methodName
    val lineNumber =
        Thread.currentThread().stackTrace[3].lineNumber
    android.util.Log.e(
        "[$className], [$methodName], [$lineNumber] ",
        "\n${message ?: "null"}"
    )
}
//    }
}