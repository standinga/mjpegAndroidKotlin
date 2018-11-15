package borama.co.mjpegstream

class MjpegStream {

    var running = false

    fun startStream(url: String, handler: (MjpegData?, Exception?)->Unit) {
        running = true
        MjpegInputStream.read(url, object: MjpegInputStream.MJpegListener {
            override fun onError(exception: Exception) {
                handler(null, exception)
            }

            override fun onStream(stream: MjpegInputStream) {
                val mjpegTread = Thread(Runnable {
                    while (running) {
                        try {
                            val data = stream.readMjpegFrame()
                            handler(data, null)
                        } catch (exception: Exception) {
                            handler(null, exception)
                        }
                    }
                })
                mjpegTread.start()
            }
        })
    }

    fun stopStream () {
        running = false
    }
}