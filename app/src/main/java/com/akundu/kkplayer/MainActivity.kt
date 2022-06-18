package com.akundu.kkplayer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType.UNMETERED
import androidx.work.OneTimeWorkRequest.Builder
import androidx.work.WorkManager
import com.akundu.kkplayer.data.Song
import com.akundu.kkplayer.data.SongDataProvider
import com.akundu.kkplayer.ui.theme.KkPlayerTheme
import com.akundu.kkplayer.work.DownloadWork
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KkPlayerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    SongListPreview()
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .background(Color(0xFFF3D3C8))
            .clickable { playSong(context, song.fileName) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(id = getDrawable(song.movie)),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(88.dp)
                //.clip(CircleShape)
                .padding(12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, style = MaterialTheme.typography.h6)
            Row() {
                Text(
                    stringResource(id = R.string.artist),
                    style = MaterialTheme.typography.body2,
                    color = Color.DarkGray
                )
                Text(
                    text = song.artist, style = MaterialTheme.typography.body2,
                    color = Color.DarkGray
                )
            }
        }
        IconButton(
            onClick = { download(context, song.fileName, song.movie) }
        ) {
            Icon(
                painterResource(id = R.drawable.ic_download),
                contentDescription = "Download",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .padding(8.dp),
                tint = Color.Blue
            )
        }
    }
}

fun playSong(context: Context, fileName: String) {
    Logg.i("FileName: $fileName")

    try {
        val uriString: String = File("/storage/emulated/0/Android/data/com.akundu.kkplayer/files/${fileName}").toString()

        val songFile = File(uriString)
        val isFileExist = songFile.exists()
        if (isFileExist) {
            Logg.i("File exist: $isFileExist")

            //val mPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.tu_hi_meri_shab_hai)
            val mPlayer: MediaPlayer = MediaPlayer.create(context, Uri.parse(uriString))
            mPlayer.start()
        } else {
            Logg.e("File exist: $isFileExist")
            Logg.e("UriString: $uriString")

            Toast.makeText(context, "Please download", Toast.LENGTH_LONG).show()
        }

    } catch (e: FileNotFoundException) {
        Logg.e("$fileName not found. Cause ${e.localizedMessage}")
        Toast.makeText(context, "Please download", Toast.LENGTH_LONG).show()

    } catch (e: NullPointerException) {
        Logg.e("Cause ${e.localizedMessage}")
        Toast.makeText(context, "Please download", Toast.LENGTH_LONG).show()
    }
}


fun download(context: Context, fileName: String, movie: String) {
    Logg.i("Downloading: $fileName")
    //Toast.makeText(context, "Downloading: $fileName", Toast.LENGTH_LONG).show()

    val notificationID = (Math.random() * 10000000000000).toInt()
    val data: Data = Data.Builder()
        .putString("fileName", fileName)
        .putString("movie", movie)
        .putInt("notificationID", notificationID)
        .build()

    val constraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(UNMETERED)
        .build()

    val request = Builder(DownloadWork::class.java)
        .setInputData(data)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueue(request)

    AppsNotificationManager.getInstance(context)?.downloadingNotification(
        targetNotificationActivity = MainActivity::class.java,
        channelId = "CHANNEL_ID",
        title = fileName,
        text = "Downloading",
        bigText = "",
        notificationId = notificationID,
        drawableId = getDrawable(movie)
    )
}


@Throws(IOException::class)
fun copyInputStreamToFile(inputStream: InputStream, file: File) {

    FileOutputStream(file, false).use { outputStream ->
        var read: Int
        val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
        while (inputStream.read(bytes).also { read = it } != -1) {
            outputStream.write(bytes, 0, read)
        }
    }
}

@Preview
@Composable
fun SongItemPreview() {
    SongItem(SongDataProvider.kkSongList[0])
}


@Composable
fun SongListCompose(
    songList: List<Song>,
    modifier: Modifier = Modifier
) {
    // Use LazyRow when making horizontal lists
    LazyColumn(modifier = modifier) {
        items(songList) { song ->
            SongItem(song = song)
        }
    }
}


@Preview
@Composable
fun SongListPreview() {
    SongListCompose(SongDataProvider.kkSongList)
}


private fun getDrawable(movie: String): Int {
    return when (movie) {
        "Gangster" -> R.drawable.gangster
        "Jannat" -> R.drawable.jannat
        "Woh Lamhe" -> R.drawable.woh_lamhe
        "Bajrangi Bhaijaan" -> R.drawable.bajrangi_bhaijaan
        "Kites" -> R.drawable.kites
        "Live-The Train" -> R.drawable.the_train
        else -> R.drawable.ic_music
    }
}

