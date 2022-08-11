package org.mrfyo.rpc.core.server;

import org.mrfyo.rpc.core.service.HelloService;

import java.io.IOException;
import java.util.concurrent.Executors;

public class TestServer {

    public static void main(String[] args) {
        try (RpcServer server = new RpcServer(9000, Executors.newCachedThreadPool())) {
            server.register(HelloService.class, new HelloServiceImpl());
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
