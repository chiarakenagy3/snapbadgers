package com.example.snapbadgers.ai.text.ml

import android.content.Context
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ModelLoader {
    fun loadMappedFile(context: Context, assetName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetName)
        return try {
            val inputStream = fileDescriptor.createInputStream()
            try {
                val fileChannel = inputStream.channel
                fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            } finally {
                inputStream.close()
            }
        } finally {
            fileDescriptor.close()
        }
    }
}
