package org.mrfyo.rpc.core.client;

import org.mrfyo.rpc.core.service.HelloService;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class TestClient {

    public static void main(String[] args) throws IOException {
        RpcTransfer transfer = new RpcTransfer("127.0.0.1", 9000, 5);
        RpcClient client = new RpcClient(transfer);
        HelloService helloService = client.getService(HelloService.class);

        for (int i = 0; i < 10; i++) {
            ForkJoinPool.commonPool().execute(() -> {
                System.out.println(helloService.hello("Jack"));
            });
        }
        ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.SECONDS);
        client.close();
    }
}
