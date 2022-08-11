package org.mrfyo.rpc.core.client;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Proxy;

/**
 * RPC 客户端
 */
public class RpcClient implements Closeable {
    /**
     * RPC 传输器
     */
    private final RpcTransfer transfer;

    public RpcClient(RpcTransfer transfer) {
        this.transfer = transfer;
    }

    /**
     * 获取代理服务对象
     *
     * @param serviceType 服务类型对象
     * @param <T>         服务类型
     * @return 代理服务对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceType) {
        return (T) Proxy.newProxyInstance(serviceType.getClassLoader(),
                new Class[]{serviceType},
                new RpcServiceProxyInvoker(transfer, serviceType));
    }

    @Override
    public void close() throws IOException {
        transfer.close();
    }
}
