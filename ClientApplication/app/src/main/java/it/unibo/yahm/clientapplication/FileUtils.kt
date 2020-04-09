package it.unibo.yahm.clientapplication

import android.content.Context
import android.util.Log
import java.io.*
import java.lang.StringBuilder

class FileUtils(fileName: String, context: Context) {
    //can throw java.io.FileNotFoundException
    private val outputStreamWriter = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_APPEND))
    private val inputStream: InputStream = context.openFileInput(fileName)

    //can throw java.io.IOException
    fun writeLine(line: String) {
        outputStreamWriter.write(line + "\n")
    }

    //can throw java.io.IOException
    fun writeLines(lines: List<String>) {
        lines.forEach { writeLine(it) }
    }

    fun readLines(): String {
        val stringBuilder = StringBuilder()
        val inputStreamReader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(inputStreamReader)

        for (line in bufferedReader.lines()) {
            stringBuilder.append("\n").append(line);
        }
        return stringBuilder.toString()
    }

    fun close() {
        outputStreamWriter.close()
        inputStream.close()
    }
}