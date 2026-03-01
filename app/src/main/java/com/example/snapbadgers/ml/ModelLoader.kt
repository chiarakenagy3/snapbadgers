package com.example.snapbadgers.ml

import android.content.Context
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ModelLoader {
    fun loadMappedFile(context: Context, assetName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetName)
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        ).also {
            inputStream.close()
            fileDescriptor.close()
        }
    }
}
