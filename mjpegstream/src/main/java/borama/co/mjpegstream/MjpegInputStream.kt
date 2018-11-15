package borama.co.mjpegstream

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MjpegInputStream(val input: InputStream) : DataInputStream(BufferedInputStream(input, FRAME_MAX_LENGTH)) {


    interface MJpegListener {
        fun onStream(stream: MjpegInputStream)
        fun onError(exception: Exception)
    }


    private var mContentLength = -1

    private class StreamRunnable internal constructor(internal var inputStream: InputStream, internal var sListener: MJpegListener) : Runnable {
        override fun run() {
            sListener.onStream(MjpegInputStream(inputStream))
        }
    }

    @Throws(IOException::class)
    private fun getEndOfSeqeunce(dataInputStream: DataInputStream, sequence: ByteArray): Int {

        var seqIndex = 0
        var c: Byte
        for (i in 0 until FRAME_MAX_LENGTH) {
            c = dataInputStream.readUnsignedByte().toByte()
            if (c == sequence[seqIndex]) {
                seqIndex++
                if (seqIndex == sequence.size) return i + 1
            } else
                seqIndex = 0
        }
        return -1
    }

    @Throws(IOException::class)
    private fun getStartOfSequence(dataInputStream: DataInputStream, sequence: ByteArray): Int {

        val end = getEndOfSeqeunce(dataInputStream, sequence)
        return if (end < 0) -1 else end - sequence.size
    }

    @Throws(IOException::class, NumberFormatException::class)
    private fun parseContentLength(headerBytes: ByteArray): Int {

        val headerIn = ByteArrayInputStream(headerBytes)
        val props = Properties()
        props.load(headerIn)
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH))
    }

    @Throws(IOException::class)
    fun readMjpegFrame(): MjpegData {
        mark(FRAME_MAX_LENGTH)
        val headerLen = getStartOfSequence(this, SOI_MARKER)
        reset()
        val header = ByteArray(headerLen)
        readFully(header)
        try {
            mContentLength = parseContentLength(header)
        } catch (nfe: NumberFormatException) {
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER)
        }

        reset()
        val frameData = ByteArray(mContentLength)

        skipBytes(headerLen)
        readFully(frameData)

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(ByteArrayInputStream(frameData), null, options)
        val width = options.outWidth
        val height = options.outHeight

        return MjpegData(frameData, width, height)
    }
    companion object {
        private val SOI_MARKER = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        private val EOF_MARKER = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
        private val CONTENT_LENGTH = "Content-Length"
        private val HEADER_MAX_LENGTH = 100
        private val FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH
        const val TAG = " MjpegInputStream"

        fun read(urlString: String, listener: MJpegListener): MjpegInputStream? {
            Thread(Runnable {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "")
                    connection.requestMethod = "GET"
                    connection.doInput = true
                    connection.connect()
                    val inputStream = connection.inputStream
                    val looper = Looper.getMainLooper()
                    val handler = Handler(looper)
                    handler.post(StreamRunnable(inputStream, listener))
                } catch (e: IOException) {
                    e.printStackTrace()
                    listener.onError(e)
                }
            }).start()

            return null
        }
    }
}