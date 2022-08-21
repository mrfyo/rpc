package org.mrfyo.rpc.core.server;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.mrfyo.rpc.core.codec.RpcRequest;
import org.mrfyo.rpc.core.codec.RpcResponse;
import org.mrfyo.rpc.core.protocol.RpcRequestBody;
import org.mrfyo.rpc.core.protocol.RpcResponseBody;

import java.io.*;
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

    private final Kryo kryo;


    public RpcServiceHandler(Socket socket, Map<String, Object> servicePool) {
        this.socket = socket;
        this.servicePool = servicePool;
        this.kryo = new Kryo();
        this.kryo.setRegistrationRequired(false);
    }

    @Override
    public void run() {
        exec();
    }

    private void exec() {
        while (true) {
            try {
                // 0. 读取头部
                int encodeType = socket.getInputStream().read();
                if (encodeType == -1) {
                    System.err.println("bye " + socket.getRemoteSocketAddress());
                    break;
                }
                System.out.println("handle remote calling...");
                // 1. 解码
                RpcRequest request;
                try {
                    Object obj;
                    switch (encodeType) {
                        case 1 -> {
                            obj = new ObjectInputStream(socket.getInputStream()).readObject();
                        }
                        case 2 -> {
                            obj = kryo.readClassAndObject(new Input(socket.getInputStream()));
                        }
                        case 3 -> {
                            DataInputStream dos = new DataInputStream(socket.getInputStream());
                            int length = dos.readInt();
                            byte[] b = new byte[length];
                            if (dos.read(b) != length) {
                                System.err.println("codec protocol of client mismatches server");
                            }
                            obj = JSON.parseObject(b, RpcRequest.class);
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + encodeType);
                    }
                    request = (RpcRequest) obj;
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

                    int paramSize = body.getParamTypes().size();
                    Class<?>[] paramTypes = new Class[paramSize];
                    for (int i = 0; i < paramSize; i++) {
                        paramTypes[i] = Class.forName(body.getParamTypes().get(i));
                    }

                    Method method = service.getClass().getDeclaredMethod(body.getMethodName(), paramTypes);
                    result = method.invoke(service, body.getParams());
                } catch (Exception e) {
                    System.err.println("service invoke failed.");
                }

                // 4. 写入头部
                socket.getOutputStream().write(encodeType);

                // 5. 编码
                RpcResponseBody responseBody = new RpcResponseBody();
                responseBody.setReturnObject(result);

                RpcResponse response = new RpcResponse();
                response.setHeader("version=1");
                response.setBody(responseBody);

                switch (encodeType) {
                    case 1 -> {
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        oos.writeObject(response);
                        oos.flush();
                    }
                    case 2 -> {
                        Output output = new Output(socket.getOutputStream());
                        kryo.writeClassAndObject(output, response);
                        output.flush();
                    }
                    case 3 -> {
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        byte[] b = JSON.toJSONBytes(response);
                        dos.writeInt(b.length);
                        dos.write(b);
                        dos.flush();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + encodeType);
                }

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
