package com.example.batterymonitor

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtils {
    private const val LOG_FILE_NAME = "battery_monitor.log"
    private const val MAX_LOG_SIZE = 50 * 1024 // 50KB
    private const val MAX_LOG_LINES = 1000
    
    fun log(tag: String, message: String) {
        // 打印到控制台
        Log.d(tag, message)
        
        // 写入文件
        writeToFile("[${getCurrentTime()}] [$tag] $message")
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        // 打印到控制台
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        
        // 写入文件
        val errorMessage = if (throwable != null) {
            "[${getCurrentTime()}] [$tag] ERROR: $message\n${throwable.stackTraceToString()}"
        } else {
            "[${getCurrentTime()}] [$tag] ERROR: $message"
        }
        writeToFile(errorMessage)
    }
    
    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
    
    private fun writeToFile(message: String) {
        try {
            val logFile = getLogFile()
            
            // 检查文件大小
            if (logFile.length() > MAX_LOG_SIZE) {
                // 截断文件，保留最后MAX_LOG_LINES行
                truncateLogFile(logFile)
            }
            
            // 追加写入
            FileWriter(logFile, true).use {
                it.write(message)
                it.write("\n")
            }
        } catch (e: IOException) {
            Log.e("LogUtils", "Failed to write log: ${e.message}")
        }
    }
    
    private fun getLogFile(): File {
        val context = AppContextProvider.getContext()
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return File(logDir, LOG_FILE_NAME)
    }
    
    private fun truncateLogFile(file: File) {
        try {
            val lines = file.readLines()
            val linesToKeep = if (lines.size > MAX_LOG_LINES) {
                lines.takeLast(MAX_LOG_LINES)
            } else {
                lines
            }
            
            FileWriter(file, false).use {
                linesToKeep.forEach {line ->
                    it.write(line)
                    it.write("\n")
                }
            }
        } catch (e: IOException) {
            Log.e("LogUtils", "Failed to truncate log: ${e.message}")
        }
    }
    
    fun getLogContent(): String {
        try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                return logFile.readText()
            }
        } catch (e: IOException) {
            Log.e("LogUtils", "Failed to read log: ${e.message}")
        }
        return "暂无日志"
    }
    
    fun clearLogs() {
        try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                logFile.writeText("")
            }
        } catch (e: IOException) {
            Log.e("LogUtils", "Failed to clear log: ${e.message}")
        }
    }
}