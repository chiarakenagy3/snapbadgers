package com.example.snapbadgers

import android.util.Log
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.model.Song
import kotlin.math.sqrt

class RecommendationService(
    private val songLibrary: List<Song>
) {

    data class RecommendationResult(
        val song: Song,
        val finalScore: Float,
        val textScore: Float,
        val visionScore: Float,
        val userScore: Float
    )

    fun recommendSongs(
        textEmbedding: FloatArray? = null,
        visionEmbedding: FloatArray? = null,
        userEmbedding: FloatArray? = null,
        textWeight: Float = 0.4f,
        visionWeight: Float = 0.4f,
        userWeight: Float = 0.2f,
        limit: Int = 10
    ): List<RecommendationResult> {

        fun norm(v: FloatArray): Float {
            var s = 0f
            for (x in v) s += x * x
            return sqrt(s)
        }

        Log.d("RecService", "==============================")
        Log.d("RecService", "START RECOMMENDATION")
        Log.d("RecService", "songLibrary size=${songLibrary.size}")

        textEmbedding?.let {
            Log.d(
                "RecService",
                "QUERY TEXT dim=${it.size} norm=${norm(it)} head=${it.take(8)}"
            )
        }

        visionEmbedding?.let {
            Log.d(
                "RecService",
                "QUERY VISION dim=${it.size} norm=${norm(it)} head=${it.take(8)}"
            )
        }

        userEmbedding?.let {
            Log.d(
                "RecService",
                "QUERY USER dim=${it.size} norm=${norm(it)} head=${it.take(8)}"
            )
        }

        // 查看前5首歌 embedding
        songLibrary.take(5).forEachIndexed { idx, song ->
            val e = song.embedding
            Log.d(
                "RecService",
                "SONG[$idx] title=${song.title} dim=${e.size} norm=${norm(e)} head=${e.take(8)}"
            )
        }

        val normalizedText = textEmbedding?.let { VectorUtils.normalize(it.copyOf()) }
        val normalizedVision = visionEmbedding?.let { VectorUtils.normalize(it.copyOf()) }
        val normalizedUser = userEmbedding?.let { VectorUtils.normalize(it.copyOf()) }

        var kept = 0
        var droppedEmpty = 0
        var droppedDim = 0

        val results = songLibrary.mapNotNull { song ->

            if (song.embedding.isEmpty()) {
                droppedEmpty++
                return@mapNotNull null
            }

            val songEmbRaw = song.embedding

            if (normalizedText != null && songEmbRaw.size != normalizedText.size) {
                droppedDim++
                Log.e(
                    "RecService",
                    "DIM MISMATCH TEXT song=${song.title} songDim=${songEmbRaw.size} textDim=${normalizedText.size}"
                )
                return@mapNotNull null
            }

            if (normalizedVision != null && songEmbRaw.size != normalizedVision.size) {
                droppedDim++
                Log.e(
                    "RecService",
                    "DIM MISMATCH VISION song=${song.title} songDim=${songEmbRaw.size} visionDim=${normalizedVision.size}"
                )
                return@mapNotNull null
            }

            if (normalizedUser != null && songEmbRaw.size != normalizedUser.size) {
                droppedDim++
                Log.e(
                    "RecService",
                    "DIM MISMATCH USER song=${song.title} songDim=${songEmbRaw.size} userDim=${normalizedUser.size}"
                )
                return@mapNotNull null
            }

            kept++

            val songEmb = VectorUtils.normalize(songEmbRaw.copyOf())

            var scoreSum = 0f
            var activeWeightSum = 0f

            val tScore = normalizedText?.let {
                val s = VectorUtils.dot(it, songEmb)
                scoreSum += s * textWeight
                activeWeightSum += textWeight
                s
            } ?: 0f

            val vScore = normalizedVision?.let {
                val s = VectorUtils.dot(it, songEmb)
                scoreSum += s * visionWeight
                activeWeightSum += visionWeight
                s
            } ?: 0f

            val uScore = normalizedUser?.let {
                val s = VectorUtils.dot(it, songEmb)
                scoreSum += s * userWeight
                activeWeightSum += userWeight
                s
            } ?: 0f

            val finalScore =
                if (activeWeightSum > 1e-8f) scoreSum / activeWeightSum else 0f

            RecommendationResult(
                song = song,
                finalScore = finalScore,
                textScore = tScore,
                visionScore = vScore,
                userScore = uScore
            )
        }
            .sortedByDescending { it.finalScore }

        Log.d(
            "RecService",
            "FILTER RESULT kept=$kept droppedEmpty=$droppedEmpty droppedDim=$droppedDim"
        )

        results.take(10).forEachIndexed { idx, r ->
            Log.d(
                "RecService",
                "#$idx ${r.song.title} | final=${r.finalScore} text=${r.textScore} vision=${r.visionScore} user=${r.userScore}"
            )
        }

        Log.d("RecService", "END RECOMMENDATION")
        Log.d("RecService", "==============================")

        return results.take(limit)
    }
}