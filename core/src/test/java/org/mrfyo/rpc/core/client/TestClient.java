package org.mrfyo.rpc.core.client;

import org.mrfyo.rpc.core.service.HelloService;

import java.io.IOException;

public class TestClient {

    public static void main(String[] args) throws IOException {
        try(Transfer transfer = new NioRpcTransfer("127.0.0.1", 9000, 3, 5)) {
            RpcClient client = new RpcClient(transfer);
            HelloService helloService = client.getService(HelloService.class);
            System.out.println(helloService.hello("Jack"));
        }
//        for (int i = 0; i < 10; i++) {
//            ForkJoinPool.commonPool().execute(() -> {
//                System.out.println(helloService.hello("Jack"));
//            });
//        }
//        ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.SECONDS);
////        client.close();
    }
}
