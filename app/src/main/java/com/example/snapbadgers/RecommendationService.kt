package com.example.snapbadgers

import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.model.Song

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

        val normalizedText = textEmbedding?.let { VectorUtils.normalize(it) }
        val normalizedVision = visionEmbedding?.let { VectorUtils.normalize(it) }
        val normalizedUser = userEmbedding?.let { VectorUtils.normalize(it) }

        return songLibrary.map { song ->
            // Song embeddings should already be normalized, but we ensure it for safety
            val songEmb = VectorUtils.normalize(song.embedding)

            var scoreSum = 0f
            var activeWeightSum = 0f

            // Calculate similarity scores
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

            // Final score is the weighted average of available modalities
            val finalScore = if (activeWeightSum > 1e-8f) {
                scoreSum / activeWeightSum
            } else {
                0f
            }

            RecommendationResult(
                song = song,
                finalScore = finalScore,
                textScore = tScore,
                visionScore = vScore,
                userScore = uScore
            )
        }
        .filter { it.song.embedding.isNotEmpty() } // Filter out invalid entries
        .sortedByDescending { it.finalScore }
        .take(limit)
    }
}
