package io.buildfoundation.kaliy.http.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpHeaderNames.*
import io.netty.handler.codec.http.HttpHeaderValues.CLOSE
import io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.reactivex.Completable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

fun httpServer(port: Int): Completable {
    return Completable.defer {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()

        val b = ServerBootstrap()
        b.option(ChannelOption.SO_BACKLOG, 1024)
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(Initializer())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
//                .childOption(ChannelOption.AUTO_READ, false)

        Completable.fromFuture(b.bind(port))
                .doOnDispose {
                    bossGroup.shutdownGracefully()
                    workerGroup.shutdownGracefully()
                }
//        .().channel().closeFuture().sync()
    }
}


private class Initializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        with(ch.pipeline()) {
            addLast(HttpServerCodec())
            addLast(HttpServerExpectContinueHandler())
            addLast(Handler())
        }
    }

}

private class Handler : SimpleChannelInboundHandler<HttpObject>() {
    val array = ByteArray(4 * 1024)

    var total = 0

    var last = false

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
        last = msg is LastHttpContent

        if (msg is HttpContent) {
            val content = msg.content()

            var available = content.readableBytes()
            total += available
            while (available > 0) {
                content.readBytes(array, 0, min(array.size, available))
                available = content.readableBytes()
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        super.channelReadComplete(ctx)
        if (last) ctx.send("Pe$total")
    }

    private fun ChannelHandlerContext.send(msg: String) {
        val buf = alloc().ioBuffer()
        buf.writeBytes(msg.toByteArray())

        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK, buf)


        response.headers().set(CONNECTION, CLOSE)

        response.headers()
                .set(CONTENT_TYPE, TEXT_PLAIN)
                .setInt(CONTENT_LENGTH, response.content().readableBytes())

        response.content()

        val writeAndFlush = writeAndFlush(response)

        writeAndFlush.addListener(ChannelFutureListener.CLOSE)

        if (buf.refCnt() > 0) buf.release()
    }

}
