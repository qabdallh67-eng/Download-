package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.adapters.DirectUrlAdapter
import com.example.data.adapters.FacebookAdapter
import com.example.data.adapters.InstagramAdapter
import com.example.data.adapters.MediaAnalyzerService
import com.example.data.adapters.TelegramAdapter
import com.example.data.adapters.TikTokAdapter
import com.example.data.adapters.VimeoAdapter
import com.example.data.adapters.WhatsAppAdapter
import com.example.data.adapters.XAdapter
import com.example.data.adapters.YouTubeAdapter
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaDownloadType
import com.example.data.model.PlatformType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches official branding`() {
        assertEquals("عبدالله أبو قاعود", context.getString(R.string.app_name))
        assertEquals("Abdullah Abu Qaoud", context.getString(R.string.app_name_english))
    }

    @Test
    fun `extractFirstUrl parses shared text from social apps`() {
        val analyzer = MediaAnalyzerService()
        val sharedText = "شاهد هذا الفيديو الرائع https://www.facebook.com/watch/?v=1234567890 عبر فيسبوك"
        assertEquals(
            "https://www.facebook.com/watch/?v=1234567890",
            analyzer.extractFirstUrl(sharedText)
        )
    }

    @Test
    fun `all platform adapters match their official domains`() {
        assertTrue(FacebookAdapter().matches("https://fb.watch/abc123/"))
        assertTrue(TikTokAdapter().matches("https://vm.tiktok.com/ZM123/"))
        assertTrue(InstagramAdapter().matches("https://www.instagram.com/reel/xyz/"))
        assertTrue(YouTubeAdapter().matches("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(XAdapter().matches("https://x.com/user/status/123"))
        assertTrue(VimeoAdapter().matches("https://vimeo.com/76979871"))
        assertTrue(TelegramAdapter().matches("https://t.me/channel/42"))
        assertTrue(WhatsAppAdapter().matches("content://com.whatsapp.provider.media/item/1", "video/mp4"))
        assertTrue(DirectUrlAdapter().matches("https://example.com/sample_video.mp4"))
    }

    @Test
    fun `room download dao supports insert progress completion favorite and rename`() = runTest {
        val dao = db.downloadDao()
        val id = dao.insert(
            DownloadEntity(
                title = "Sample Clip",
                sourceUrl = "https://example.com/clip.mp4",
                resolvedMediaUrl = "https://example.com/clip.mp4",
                platform = PlatformType.DIRECT_URL.name,
                mediaType = MediaDownloadType.VIDEO.name,
                qualityLabel = "Full HD – 1080p",
                formatExt = "mp4",
                fileSizeBytes = 10_000L
            )
        )

        dao.updateProgress(id, DownloadStatus.DOWNLOADING.name, 5_000L, 10_000L, 1_000L, 5L, null)
        val mid = dao.getById(id)
        assertNotNull(mid)
        assertEquals(50, mid!!.progressPercent)

        dao.markCompleted(id, 10_000L, "/tmp/clip.mp4", null, System.currentTimeMillis())
        dao.setFavorite(id, true)
        dao.updateTitleAndPath(id, "Renamed Clip", "/tmp/Renamed Clip.mp4")

        val favs = dao.getFavorites().first()
        assertEquals(1, favs.size)
        assertEquals("Renamed Clip", favs.first().title)
        assertEquals(DownloadStatus.COMPLETED, favs.first().statusEnum)
    }

    @Test
    fun `quick share activity launches cleanly on ACTION_SEND`() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        }
        val controller = Robolectric.buildActivity(QuickShareActivity::class.java, shareIntent).create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
