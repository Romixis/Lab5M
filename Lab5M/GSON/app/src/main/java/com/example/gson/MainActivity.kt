package com.example.gson

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import timber.log.Timber

data class Photo (
    val id: Long,
    val owner: String = "",
    val secret: String = "",
    val server: Int = 0,
    val farm: Int = 0,
    val title: String = "",
    val ispublic: Int = 1,
    val isfriend: Int = 0,
    val isfamily: Int = 1
)

data class PhotoPage (
    val page: Int = 1,
    val pages: Int = 1081,
    val perpage: Int = 100,
    val total: Int = 108063,
    val photo: JsonArray
)

data class Wrapper (
    val photos: JsonObject,
    val stat: String = "ok"
)


class Adapter (private val context: Context,
               private val list: ArrayList<String>,
               private val cellClickListener: MainActivity) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.rview_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]
        val `in`: InputStream = URL(data).openStream()
        val bmp = BitmapFactory.decodeStream(`in`)
        holder.image.setImageBitmap(bmp)

        holder.itemView.setOnClickListener {
            cellClickListener.onCellClickListener(data)
        }
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        val samples = arrayListOf<String>()
        val recyclerView: RecyclerView = findViewById(R.id.rView)

        val flickr = "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=ff49fcd4d4a08aa6aafb6ea3de826464&tags=cat&format=json&nojsoncallback=1"

        Thread {
            val connection = URL(flickr).openConnection() as HttpURLConnection

            val jsonData = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val wrappedPhotos: Wrapper = Gson().fromJson(jsonData, Wrapper::class.java)
            val firstPage: PhotoPage = Gson().fromJson(wrappedPhotos.photos, PhotoPage::class.java)
            val photos = Gson().fromJson(firstPage.photo, Array<Photo>::class.java).toList()

            for (i in photos.indices) {
                if (i.mod(5) == 4) {
                    Timber.d(photos[i].toString())
                }
                samples.add("https://farm${photos[i].farm}.staticflickr.com/${photos[i].server}/${photos[i].id}_${photos[i].secret}_z.jpg")
            }
            connection.disconnect()

            runOnUiThread() {
                recyclerView.layoutManager = GridLayoutManager(this,2)
                recyclerView.adapter = Adapter(this, samples, this)
            }
        }.start()
    }

    fun onCellClickListener(data: String) {
        val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Photo link", data)
        Timber.i(data)
        clipboard.setPrimaryClip(clip)
    }
}