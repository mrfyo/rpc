package org.mrfyo.rpc.core.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.mrfyo.rpc.core.codec.*;
import org.mrfyo.rpc.core.invoker.SimpleRpcInvoker;
import org.mrfyo.rpc.core.protocol.RpcRequestBody;
import org.mrfyo.rpc.core.protocol.RpcResponseBody;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyRpcServer implements RpcServer {

    private final int port;

    private final Map<String, Object> servicePool = new ConcurrentHashMap<>();

    private final Map<String, Object> readonlyServicePool = Collections.unmodifiableMap(servicePool);

    public NettyRpcServer(int port) {
        this.port = port;
    }

    @Override
    public <T> void register(Class<T> serviceType, T service) {
        servicePool.put(serviceType.getName(), service);
    }

    @Override
    public <T> void unregister(Class<T> serviceType) {
        servicePool.remove(serviceType.getName());
    }

    @Override
    public void run() {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            ChannelFuture f = b.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline p = channel.pipeline();
    //                        p.addLast(new LengthFieldBasedFrameDecoder(1024, 1, 4));
                            p.addLast(new RpcInboundHandler(readonlyServicePool));
                        }
                    }).bind(port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {

    }


    private static final class RpcInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

        /**
         * 本地 服务池
         */
        private final Map<String, Object> servicePool;

        public RpcInboundHandler(Map<String, Object> servicePool) {
            this.servicePool = servicePool;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf inBuf) throws Exception {
            // 1. 读取头部
            int encodeType = inBuf.readByte();
            if (encodeType == -1) {
                System.err.println("bye " + ctx.channel().remoteAddress());
                return;
            }
            int length = inBuf.readInt();
            System.out.println("Netty handle remote calling...");
            byte[] b = new byte[length];
            inBuf.readBytes(b);
            // 2. 解码
            RpcRequest request = selectRcpCodec(encodeType).decode(b, RpcRequest.class);

            if (!"version=1".equals(request.getHeader())) {
                return;
            }
            RpcRequestBody body = request.getBody();
            if (body == null) {
                System.err.println("the codec protocol of client mismatches server.");
                return;
            }

            // 2. 执行本地方法
            RpcResponseBody responseBody = new SimpleRpcInvoker(servicePool).invoke(request.getBody());

            // 3. 编码
            RpcResponse response = new RpcResponse();
            response.setHeader("version=1");
            response.setBody(responseBody);
            b = selectRcpCodec(encodeType).encode(response);

            // 4. 传输
            ByteBuf outBuf = Unpooled.buffer(b.length + 16);
            outBuf.writeByte(encodeType);
            outBuf.writeInt(b.length);
            outBuf.writeBytes(b);
            ctx.writeAndFlush(outBuf);
        }

        private RpcCodec selectRcpCodec(int type) {
            return switch (type) {
                case 1 -> new JdkRpcCodec();
                case 2 -> new KryoRpcCodec(1);
                case 3 -> new JsonRpcCodec();
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
        }
    }
}
