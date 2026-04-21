package com.example.snapbadgers.data

import android.content.Context
import android.content.res.AssetManager
import com.example.snapbadgers.ai.common.ml.VectorUtils
import com.example.snapbadgers.ai.text.HeuristicTextEmbedding
import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/**
 * JVM tests for the offline/fallback branches of SongRepository.
 * Exercises the path where no embedded catalog, no sample catalog, and no on-disk
 * cache exist — the app must still return a non-empty recommendation list.
 */
class SongRepositoryTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var assets: AssetManager

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        assets = mock(AssetManager::class.java)

        whenever(context.applicationContext).doReturn(context)
        whenever(context.filesDir).doAnswer { tempDir.root }
        whenever(context.assets).doReturn(assets)
        whenever(assets.open(any<String>())).doAnswer { throw IOException("no asset in fallback test") }
    }

    @Test
    fun `fallback catalog is used when no embedded or sample data is available`() {
        val repo = SongRepository(context)

        val allSongs = repo.getAllSongs()

        assertEquals("Fallback catalog size", 3, allSongs.size)
        val titles = allSongs.map { it.title }.toSet()
        assertTrue("Blinding Lights must be in fallback", "Blinding Lights" in titles)
        assertTrue("Sunflower must be in fallback", "Sunflower" in titles)
        assertTrue("Weightless must be in fallback", "Weightless" in titles)
    }

    @Test
    fun `fallback embeddings have expected dimensionality`() {
        val repo = SongRepository(context)
        val songs = repo.getAllSongs()

        val expectedDim = HeuristicTextEmbedding.encode("probe").size
        songs.forEach { song ->
            assertEquals("${song.title} embedding dim", expectedDim, song.embedding.size)
            assertTrue("${song.title} embedding must be finite", song.embedding.all { it.isFinite() })
        }
    }

    @Test
    fun `findTopSongs returns up to limit ranked by similarity`() {
        val repo = SongRepository(context)
        val query = HeuristicTextEmbedding.encode("calm study relax ambient")

        val top = repo.findTopSongs(query, limit = 2)

        assertEquals("Limit is honored", 2, top.size)
        val similarities = top.map { it.similarity }
        assertTrue(
            "Results must be sorted descending by similarity",
            similarities == similarities.sortedDescending()
        )
        assertTrue("All similarities must be finite", similarities.all { it.isFinite() })
    }

    @Test
    fun `findTopSong returns highest similarity song`() {
        val repo = SongRepository(context)
        val query = HeuristicTextEmbedding.encode("calm study relax ambient")

        val best = repo.findTopSong(query)

        val all = repo.getAllSongs()
        val normalizedQuery = VectorUtils.alignToEmbeddingDimension(query, salt = 101)
        val expected = all.maxByOrNull { VectorUtils.cosineSimilarity(normalizedQuery, it.embedding) }

        assertNotNull(expected)
        assertEquals(expected!!.title, best.title)
    }

    @Test
    fun `hasEmbeddedCatalog is false when no data available`() {
        val repo = SongRepository(context)

        assertTrue("Repo must expose no embedded catalog under fallback", !repo.hasEmbeddedCatalog)
    }

    @Test
    fun `findTopSongs coerces a zero limit to at least one result`() {
        val repo = SongRepository(context)
        val query = HeuristicTextEmbedding.encode("music")

        val result = repo.findTopSongs(query, limit = 0)

        assertEquals("limit=0 must still return at least one song", 1, result.size)
    }

    @Test
    fun `tracks file on disk is read before assets fallback`() {
        val tracksFile = File(tempDir.root, "tracks_features.json")
        tracksFile.writeText(
            """[{"trackId":"t1","name":"From Disk","artists":"Tester","embedding":[0.1,0.2,0.3,0.4]}]"""
        )

        val repo = SongRepository(context)
        val songs = repo.getAllSongs()

        val titles = songs.map { it.title }
        assertTrue("Disk-sourced track must be present: got $titles", "From Disk" in titles)
    }
}
