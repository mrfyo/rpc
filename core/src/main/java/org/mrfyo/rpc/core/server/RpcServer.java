package org.mrfyo.rpc.core.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class RpcServer implements Closeable {
    private final int port;

    private final Executor executor;

    private final Map<String, Object> servicePool = new ConcurrentHashMap<>();

    private final Map<String, Object> readonlyServicePool = Collections.unmodifiableMap(servicePool);

    private ServerSocket serverSocket;

    public RpcServer(int port, Executor executor) {
        this.port = port;
        this.executor = executor;
    }

    public <T> void register(Class<T> serviceType, T service) {
        servicePool.put(serviceType.getName(), service);
    }

    public <T> void unregister(Class<T> serviceType) {
        servicePool.remove(serviceType.getName());
    }


    public void start() throws IOException {
        if (serverSocket != null) {
            return;
        }
        serverSocket = new ServerSocket(port);
        System.out.println("server listen: " + serverSocket.getLocalSocketAddress());
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("welcome " + socket.getRemoteSocketAddress());
                executor.execute(new RpcServiceHandler(socket, readonlyServicePool));
            } catch (Exception e) {
                break;
            }
        }
    }


    @Override
    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
