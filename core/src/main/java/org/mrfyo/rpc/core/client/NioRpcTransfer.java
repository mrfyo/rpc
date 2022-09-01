package org.mrfyo.rpc.core.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.AttributeKey;
import org.mrfyo.rpc.core.codec.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class NioRpcTransfer implements Transfer {

    /**
     * 服务端主机地址
     */
    private final String host;

    /**
     * 服务端端口
     */
    private final int port;

    /**
     * Channel 最大个数
     */
    private final int cap;

    private final int encodeType;

    /**
     * Channel 阻塞队列
     */
    private final BlockingQueue<Channel> channelQueue;

    private final EventLoopGroup worker = new NioEventLoopGroup();

    /**
     * Channel 当前个数
     */
    private int size;

    private boolean closed;

    public NioRpcTransfer(String host, int port, int encodeType, int cap) {
        assert cap > 0;
        this.host = host;
        this.port = port;
        this.encodeType = encodeType;
        this.cap = cap;
        this.channelQueue = new LinkedBlockingQueue<>(cap + 1);
        new MonitorThread().start();

    }

    // Channel Manage

    private Channel obtainChannel() {
        try {
            if (channelQueue.isEmpty()) {
                Channel channel = createChannel();
                if (channel != null) {
                    channelQueue.put(channel);
                }
            }
            return channelQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized Channel createChannel() {
        if (size < cap) {
            try {
                Channel channel = new Bootstrap().group(worker)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 1, 4));
                                ch.pipeline().addLast(new RpcMessageCodec(encodeType, cap));
                                ch.pipeline().addLast(new ClientRpcHandler());
                            }
                        })
                        .connect(host, port)
                        .sync()
                        .channel();
                size++;
                return channel;
            } catch (InterruptedException e) {
                return null;
            }
        }
        return null;
    }


    private void freeChannel(Channel channel) {
        if (channel != null && !channelQueue.offer(channel)) {
            System.err.println("channel recycles failed: " + channel.localAddress());
        }
    }

    private void closeChannel(Channel channel) {
        if (channel != null && channel.isOpen()) {
            channel.close().addListener(future -> {
                if (future.isSuccess()) {
                    System.out.println("success to close " + channel.localAddress());
                }
            });
        }
    }

    // Handle Request

    /**
     * 发送请求
     *
     * @param request RPC 请求对象
     * @return RPC 响应对象，如果调用失败，返回 null
     */
    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        boolean recycled = true;
        Channel channel = obtainChannel();
        try {
            return doSend(channel, request);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            recycled = false;
        } finally {
            if (recycled) {
                freeChannel(channel);
            } else {
                closeChannel(channel);
            }
        }
        return null;
    }

    private RpcResponse doSend(Channel channel, RpcRequest request) throws IOException {
        BlockingQueue<RpcResponse> bq = new SynchronousQueue<>(true);
        try {
            channel.attr(AttributeKey.valueOf("SQ")).set(bq);
            channel.writeAndFlush(request);
            return bq.poll(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }


    @Override
    public void close() {
        this.closed = true;
        while (!channelQueue.isEmpty()) {
            closeChannel(channelQueue.poll());
        }
        worker.shutdownGracefully();
    }

    public static class RpcMessageCodec extends ByteToMessageCodec<RpcRequest> {

        private final int encodeType;

        private final int cap;

        public RpcMessageCodec(int encodeType, int cap) {
            this.encodeType = encodeType;
            this.cap = cap;
        }

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext, RpcRequest request, ByteBuf byteBuf) throws Exception {
            byte[] b = selectRcpCodec(encodeType).encode(request);
            byteBuf.writeByte(encodeType);
            byteBuf.writeInt(b.length);
            byteBuf.writeBytes(b);
        }

        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
            int type = byteBuf.readByte();
            int len = byteBuf.readInt();
            byte[] b = new byte[len];
            byteBuf.readBytes(b);

            RpcResponse response = selectRcpCodec(type).decode(b, RpcResponse.class);
            if (response != null) {
                list.add(response);
            }

        }

        protected RpcCodec selectRcpCodec(int type) {
            return switch (type) {
                case 1 -> new JdkRpcCodec();
                case 2 -> new KryoRpcCodec(cap);
                case 3 -> new JsonRpcCodec();
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
        }
    }


    public static class ClientRpcHandler extends SimpleChannelInboundHandler<RpcResponse> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
            Object attr = ctx.channel().attr(AttributeKey.valueOf("SQ")).get();
            if (attr instanceof SynchronousQueue<?>) {
                @SuppressWarnings("unchecked")
                SynchronousQueue<RpcResponse> producer = (SynchronousQueue<RpcResponse>) attr;
                if (!producer.offer(response, 10, TimeUnit.MILLISECONDS)) {
                    System.err.println("response timeout");
                }
            }
        }
    }


    private class MonitorThread extends Thread {

        private static final int MAX_TOUCH_COUNT = 5;

        private int count;

        private int keepSize = 0;

        @Override
        public void run() {
            while (!closed) {
                if (size != keepSize) {
                    keepSize = size;
                    count = 0;
                } else {
                    count++;
                }
                if (count >= MAX_TOUCH_COUNT) {
                    count = 0;
                    Channel channel = channelQueue.poll();
                    if (channel != null) {
                        closeChannel(channel);
                    }
                    keepSize = size;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
