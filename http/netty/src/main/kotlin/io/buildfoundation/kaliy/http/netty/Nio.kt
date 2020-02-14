package io.buildfoundation.kaliy.http.netty

import io.netty.channel.EventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

fun newEventLoopPoop(): EventLoopGroup = when {
    Epoll.isAvailable() -> EpollEventLoopGroup()
    KQueue.isAvailable() -> KQueueEventLoopGroup()
    else -> NioEventLoopGroup()
}

fun serverSocketChannelClass(): Class<out ServerChannel> = when {
    Epoll.isAvailable() -> EpollServerSocketChannel::class.java
    KQueue.isAvailable() -> KQueueServerSocketChannel::class.java
    else -> NioServerSocketChannel::class.java
}
