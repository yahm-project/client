package it.unibo.yahm.client.utils

import android.content.Context
import java.io.BufferedWriter
import java.text.SimpleDateFormat
import java.util.*


class CsvFile(
    private val name: String,
    private val header: List<String>,
    private val context: Context
) {

    var fileName: String? = null
        private set

    private var bufferedWriter: BufferedWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-dd-M_HH-mm-ss", Locale.ENGLISH)

    fun open() {
        fileName = "${name}_${dateFormat.format(Date())}.csv"
        bufferedWriter = context.openFileOutput(fileName, Context.MODE_PRIVATE).bufferedWriter()
        writeValue(header)
    }

    fun writeValue(value: List<Any?>) {
        bufferedWriter ?: error("Open file first")
        if (value.size != header.size) {
            error("Value size must be equals to header size")
        }
        bufferedWriter!!.write(value.joinToString(separator = ",", postfix = "\n"))
    }

    fun close() {
        bufferedWriter ?: error("Open file first")
        bufferedWriter!!.close()
    }

}
