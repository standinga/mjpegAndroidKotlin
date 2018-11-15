package borama.co.mjpegkotlin

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import borama.co.mjpegstream.MjpegStream
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private val url = "http://192.168.0.27:8080/?action=stream"
    private val url1 = "http://dlmainstreetwebcam.gondtc.com/axis-cgi/mjpg/video.cgi"
    private var mjpegStream: MjpegStream? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        mjpegStream = MjpegStream()
        mjpegStream?.startStream(url){data, error ->
            runOnUiThread {
                if (error != null) {
                    Log.e(TAG, error.message)
                }
                if (data != null) {
                    val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(data.data))
                    Log.d(TAG, "data, w: ${bitmap.width} h: ${bitmap.height}")
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mjpegStream?.stopStream()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
