package org.mrfyo.rpc.core.client;

import org.mrfyo.rpc.core.codec.*;

import java.io.*;
import java.net.Socket;
import java.util.HexFormat;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * RPC 传输对象
 */
public class BioRpcTransfer implements Transfer {
    /**
     * 服务端主机地址
     */
    private final String host;

    /**
     * 服务端端口
     */
    private final int port;

    /**
     * Socket 最大个数
     */
    private final int cap;

    private final int encodeType;

    /**
     * Socket 阻塞队列
     */
    private final BlockingQueue<Socket> socketQueue;

    /**
     * Socket 当前个数
     */
    private int size;

    private boolean closed;

    public BioRpcTransfer(String host, int port, int encodeType, int cap) {
        assert cap > 0;
        this.host = host;
        this.port = port;
        this.encodeType = encodeType;
        this.cap = cap;
        this.socketQueue = new LinkedBlockingQueue<>(cap + 1);
        new MonitorThread().start();

    }

    // Socket Manage

    private Socket obtainSocket() {
        try {
            if (socketQueue.isEmpty()) {
                Socket socket = createSocket();
                if (socket != null) {
                    socketQueue.put(socket);
                }
            }
            return socketQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized Socket createSocket() {
        if (size < cap) {
            try {
                Socket socket = new Socket(host, port);
                size++;
                return socket;
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }


    private void freeSocket(Socket socket) {
        if (socket != null && !socketQueue.offer(socket)) {
            System.err.println("socket recycles failed: " + socket.getLocalAddress());
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("socket closes failed: " + socket.getLocalAddress());
            }
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
        Socket socket = obtainSocket();
        try {
            return doSend(socket, request);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            recycled = false;
        } finally {
            if (recycled) {
                freeSocket(socket);
            } else {
                closeSocket(socket);
            }
        }
        return null;
    }

    private RpcResponse doSend(Socket socket, RpcRequest request) throws IOException {
        byte[] b = selectRcpCodec(encodeType).encode(request);
        System.out.println(Integer.toHexString(b.length));
        System.out.println(HexFormat.ofDelimiter(" ").formatHex(b));
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.write(encodeType);
        dos.writeInt(b.length);
        dos.write(b);
        dos.flush();

        DataInputStream dis = new DataInputStream(socket.getInputStream());
        int decodeType = dis.read();
        if (decodeType == -1) {
            throw new IOException("server closed.");
        }
        int len = dis.readInt();
        b = new byte[len];
        if (dis.read(b) == len) {
            return selectRcpCodec(decodeType).decode(b, RpcResponse.class);
        } else {
            System.err.println("codec protocol of client mismatches server");
        }
        return null;
    }

    protected RpcCodec selectRcpCodec(int type) {
        return switch (type) {
            case 1 -> new JdkRpcCodec();
            case 2 -> new KryoRpcCodec(cap);
            case 3 -> new JsonRpcCodec();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    @Override
    public void close() {
        this.closed = true;
        while (!socketQueue.isEmpty()) {
            closeSocket(socketQueue.poll());
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
                    Socket socket = socketQueue.poll();
                    if (socket != null) {
                        closeSocket(socket);
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
