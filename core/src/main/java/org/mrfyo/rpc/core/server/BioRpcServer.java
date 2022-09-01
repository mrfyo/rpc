package org.mrfyo.rpc.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BioRpcServer implements RpcServer {
    private final int port;

    private final Executor executor;

    private final Map<String, Object> servicePool = new ConcurrentHashMap<>();

    private final Map<String, Object> readonlyServicePool = Collections.unmodifiableMap(servicePool);

    private ServerSocket serverSocket;

    public BioRpcServer(int port) {
        this(port, Executors.newCachedThreadPool());
    }

    public BioRpcServer(int port, Executor executor) {
        this.port = port;
        this.executor = executor;
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
        if (serverSocket != null) {
            return;
        }
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
