package io.buildfoundation.kaliy.http.netty

import com.sun.corba.se.impl.transport.ByteBufferPoolImpl
import io.buildfoundation.kaliy.config.Config
import io.buildfoundation.kaliy.http.api.HttpHandler
import io.buildfoundation.kaliy.moduleloader.ModuleLoadResult
import io.buildfoundation.kaliy.moduleloader.loadHttpHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpHeaderNames.*
import io.netty.handler.codec.http.HttpHeaderValues.CLOSE
import io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN
import io.netty.handler.codec.http.HttpResponseStatus.MULTI_STATUS
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.reactivex.Completable
import io.reactivex.exceptions.CompositeException
import okhttp3.HttpUrl
import kotlin.math.min

fun httpServer(config: Config.Http): Completable {
    return Completable.defer {
        val loadedHandlers = config.handlers.map { it to loadHttpHandler(it) }
        val errors = loadedHandlers.mapNotNull { (_, handler) -> (handler as? ModuleLoadResult.Error)?.cause }

        if (errors.isNotEmpty()) {
            return@defer Completable.error(CompositeException(errors))
        }

        val configsAndHandlers = loadedHandlers.associate { (config, result) -> config to (result as ModuleLoadResult.Ok).instance }

        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()

        val b = ServerBootstrap()
        b.option(ChannelOption.SO_BACKLOG, 1024)
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(Initializer(configsAndHandlers))
                .childOption(ChannelOption.SO_KEEPALIVE, true)
//                .childOption(ChannelOption.AUTO_READ, false)

        Completable.fromFuture(b.bind(config.port))
                .doOnDispose {
                    bossGroup.shutdownGracefully()
                    workerGroup.shutdownGracefully()
                }
//        .().channel().closeFuture().sync()
    }
}


private class Initializer(val handlers: Map<Config.Http.Handler, HttpHandler>) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        with(ch.pipeline()) {
            addLast(HttpServerCodec())
            addLast(HttpServerExpectContinueHandler())
            addLast(Handler(handlers))
        }
    }
}

private class Handler(val handlers: Map<Config.Http.Handler, HttpHandler>) : SimpleChannelInboundHandler<HttpObject>() {
    var version = HttpVersion.HTTP_1_0
    lateinit var url: HttpUrl
    var handler: HttpHandler? = null

    val array = ByteArray(4 * 1024)

    var total = 0

    var last = false

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
        last = msg is LastHttpContent
        when (msg) {
            is HttpRequest -> {
                url = HttpUrl.get("http://buildcache-stub${msg.uri()}")
                selectHandlerForUrl()
                version = msg.protocolVersion()
            }
            is HttpContent -> handleContent(ctx, msg)
        }

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

    private fun handleContent(ctx: ChannelHandlerContext, @Suppress("UNUSED_PARAMETER") msg: HttpContent) {
        if (handler == null) {
            ctx.send(DefaultFullHttpResponse(version, HttpResponseStatus.NOT_FOUND, ByteBufUtil.writeUtf8(ctx.alloc(), "Can't find handler for $url")))
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        super.channelReadComplete(ctx)
    }

    private fun ChannelHandlerContext.send(response: FullHttpResponse) {
        val buf = alloc().ioBuffer()

        response.headers().set(CONNECTION, CLOSE)

        response.headers()
                .set(CONTENT_TYPE, TEXT_PLAIN)
                .setInt(CONTENT_LENGTH, response.content().readableBytes())

        val writeAndFlush = writeAndFlush(response)

        writeAndFlush.addListener(ChannelFutureListener.CLOSE)

        buf.release()
    }

    private fun selectHandlerForUrl() {
        val pathSegments = url.pathSegments()
        handler = handlers.entries.firstOrNull { (config, _) -> pathSegments.any(config.endpoint::equals) }?.value
    }
}
