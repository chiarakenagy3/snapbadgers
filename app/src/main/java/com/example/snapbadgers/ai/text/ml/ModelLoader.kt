package com.example.snapbadgers.ai.text.ml

import android.content.Context
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ModelLoader {
    fun loadMappedFile(context: Context, assetName: String): MappedByteBuffer {
        context.assets.openFd(assetName).use { fileDescriptor ->
            fileDescriptor.createInputStream().use { inputStream ->
                return inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
    }
}
