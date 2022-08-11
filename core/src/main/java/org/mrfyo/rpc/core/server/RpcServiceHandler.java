package org.mrfyo.rpc.core.server;

import org.mrfyo.rpc.core.codec.RpcRequest;
import org.mrfyo.rpc.core.codec.RpcResponse;
import org.mrfyo.rpc.core.protocol.RpcRequestBody;
import org.mrfyo.rpc.core.protocol.RpcResponseBody;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
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
                if (socket.getInputStream().read() == -1) {
                    System.err.println("bye " + socket.getRemoteSocketAddress());
                    break;
                }
                System.out.println("handle remote calling...");
                // 1. 解码
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                RpcRequest request;
                try {
                    request = (RpcRequest) ois.readObject();
                } catch (ClassCastException | ClassNotFoundException e) {
                    System.err.println("the codec protocol of client mismatches server.");
                    continue;
                }

                if (!"version=1".equals(request.getHeader())) {
                    continue;
                }
                RpcRequestBody body = request.getBody();
                if (body == null) {
                    System.err.println("the codec protocol of client mismatches server.");
                    continue;
                }

                // 2. 执行本地方法
                Object result = null;
                try {
                    Object service = servicePool.get(body.getInterfaceName());
                    Method method = service.getClass().getDeclaredMethod(body.getMethodName(), body.getParamTypes());
                    result = method.invoke(service, body.getParams());
                } catch (Exception e) {
                    System.err.println("service invoke failed.");
                }

                // 4. 写入头部
                socket.getOutputStream().write(1);

                // 5. 编码
                RpcResponseBody responseBody = new RpcResponseBody();
                responseBody.setReturnObject(result);

                RpcResponse response = new RpcResponse();
                response.setHeader("version=1");
                response.setBody(responseBody);

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(response);
                oos.flush();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }
    }
}
