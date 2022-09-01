package org.mrfyo.rpc.core.server;

import org.mrfyo.rpc.core.service.HelloService;

import java.io.IOException;

public class TestServer {

    public static void main(String[] args) {
        try (RpcServer server = new NioRpcServer(9000)) {
            server.register(HelloService.class, new HelloServiceImpl());
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
