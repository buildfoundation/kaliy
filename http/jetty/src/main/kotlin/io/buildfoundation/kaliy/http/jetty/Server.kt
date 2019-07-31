package io.buildfoundation.kaliy.http.jetty

import io.reactivex.Completable
import org.eclipse.jetty.server.HttpInput
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import java.util.concurrent.TimeUnit
import javax.servlet.ReadListener
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun httpServer(port: Int): Completable = Completable.create { emitter ->
    val server = Server(port)
    emitter.setCancellable { server.stop() }

    val context = ServletContextHandler()
    val holder = context
            .addServlet(Servlet::class.java, "/")

    holder.isAsyncSupported = true

    server.handler = context
    server.start()

    server.join()
}

class Servlet : HttpServlet() {

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        //println(req.headerNames.toList().map { it to req.getHeader(it) })
        val asyncContext = req.startAsync()

        resp.outputStream.write("Hello".toByteArray())

        req.inputStream.setReadListener(object : ReadListener {
            val t = object : ThreadLocal<ByteArray>() {
                override fun initialValue() = ByteArray(4 * 1024)
            }
            override fun onAllDataRead() {
                resp.status = 200
                resp.outputStream.write("Pe".toByteArray())
                asyncContext.complete()
            }

            override fun onError(t: Throwable) {
                //println("~~~ onError")
                //t.printStackTrace()
            }

            override fun onDataAvailable() {
                //println("~~~ onDataAvailable thread ${Thread.currentThread().name}")

                var count = 0L
                do {
                    count += req.inputStream.read(t.get())
                } while (req.inputStream.isReady && !req.inputStream.isFinished)

                //println("~~~ onDataAvailable $count")
            }
        })

    }
}
