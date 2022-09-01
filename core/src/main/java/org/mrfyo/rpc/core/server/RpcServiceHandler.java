package org.mrfyo.rpc.core.server;

import org.mrfyo.rpc.core.codec.*;
import org.mrfyo.rpc.core.invoker.SimpleRpcInvoker;
import org.mrfyo.rpc.core.protocol.RpcRequestBody;
import org.mrfyo.rpc.core.protocol.RpcResponseBody;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * RPC 服务处理器
 */
public class RpcServiceHandler implements Runnable {

    /**
     * 本地 Socket
     */
    private final Socket socket;

    /**
     * 本地 服务池
     */
    private final Map<String, Object> servicePool;


    public RpcServiceHandler(Socket socket, Map<String, Object> servicePool) {
        this.socket = socket;
        this.servicePool = servicePool;
    }

    @Override
    public void run() {
        exec();
    }

    private void exec() {
        while (true) {
            try {
                // 0. 读取头部
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                int encodeType = dis.read();
                if (encodeType == -1) {
                    System.err.println("bye " + socket.getRemoteSocketAddress());
                    break;
                }
                System.out.println("handle remote calling... " + encodeType);
                // 读取长度
                int length = dis.readInt();
                if (length <= 0) {
                    continue;
                }
                byte[] b = new byte[length];
                if (dis.read(b) != length) {
                    continue;
                }
                // 1. 解码
                RpcRequest request = selectRcpCodec(encodeType).decode(b, RpcRequest.class);
                if (!"version=1".equals(request.getHeader())) {
                    continue;
                }
                RpcRequestBody body = request.getBody();
                if (body == null) {
                    System.err.println("the codec protocol of client mismatches server.");
                    continue;
                }

                // 2. 执行本地方法
                RpcResponseBody responseBody = new SimpleRpcInvoker(servicePool).invoke(request.getBody());

                // 3. 编码
                RpcResponse response = new RpcResponse();
                response.setHeader("version=1");
                response.setBody(responseBody);

                b = selectRcpCodec(encodeType).encode(response);
                // 4. 编码
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.write(encodeType);
                dos.writeInt(b.length);
                dos.write(b);
                dos.flush();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }
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
