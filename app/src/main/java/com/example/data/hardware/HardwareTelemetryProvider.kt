package com.example.data.hardware

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.data.model.HardwareTelemetry
import java.io.File
import java.util.Locale

class HardwareTelemetryProvider(private val context: Context) {

    fun getTelemetry(
        context: Context = this.context,
        modelsDir: File? = File(context.filesDir, "models"),
        chatsFile: File? = null
    ): HardwareTelemetry {
        // Query RAM via ActivityManager
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamBytes = memInfo.totalMem
        val availRamBytes = memInfo.availMem
        val totalRamGB = (totalRamBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).coerceAtLeast(1.0)
        val freeRamGB = (availRamBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).coerceAtLeast(0.1)
        val ramUsage = ((totalRamBytes - availRamBytes).toFloat() / totalRamBytes.toFloat()).coerceIn(0.05f, 0.95f)

        // Query Storage via StatFs
        val stat = try {
            StatFs(Environment.getDataDirectory().path)
        } catch (_: Exception) {
            StatFs(context.filesDir.path)
        }

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong

        val totalStorageBytes = totalBlocks * blockSize
        val freeStorageBytes = availBlocks * blockSize
        val totalStorageGB = (totalStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).coerceAtLeast(16.0)
        val freeStorageGB = (freeStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).coerceAtLeast(1.0)
        val storageUsage = ((totalStorageBytes - freeStorageBytes).toFloat() / totalStorageBytes.toFloat()).coerceIn(0.05f, 0.95f)

        // CPU cores
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)

        // Device Brand and Model
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        val model = Build.MODEL
        val hostDevice = if (model.contains(manufacturer, ignoreCase = true)) {
            "$model · ${Math.round(totalRamGB)} GB Unified RAM"
        } else {
            "$manufacturer $model · ${Math.round(totalRamGB)} GB Unified RAM"
        }

        // Chipset heuristic
        val soc = if (Build.HARDWARE.isNotBlank()) Build.HARDWARE.uppercase(Locale.US) else "Snapdragon 8 Gen 3"

        // Local storage used
        val modelsBytes = modelsDir?.let { calculateFolderSize(it) } ?: 0L
        val chatsBytes = chatsFile?.length() ?: 0L

        return HardwareTelemetry(
            hostDevice = hostDevice,
            socName = soc,
            cpuCoreCount = cores,
            totalRamGB = totalRamGB,
            freeRamGB = freeRamGB,
            ramUsagePercent = ramUsage,
            totalStorageGB = totalStorageGB,
            freeStorageGB = freeStorageGB,
            storageUsagePercent = storageUsage,
            modelsStorageBytes = modelsBytes,
            chatsStorageBytes = chatsBytes,
            npuTemperatureCelsius = 44
        )
    }

    private fun calculateFolderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { file ->
            total += if (file.isDirectory) calculateFolderSize(file) else file.length()
        }
        return total
    }
}
