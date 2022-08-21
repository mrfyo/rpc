package org.mrfyo.rpc.core.client;

import org.mrfyo.rpc.core.codec.RpcRequest;
import org.mrfyo.rpc.core.codec.RpcResponse;
import org.mrfyo.rpc.core.protocol.RpcRequestBody;
import org.mrfyo.rpc.core.protocol.RpcResponseBody;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RPC 服务代理执行器
 */
public class RpcServiceProxyInvoker implements InvocationHandler {
    /**
     * RPC 传输器
     */
    private final RpcTransfer transfer;

    /**
     * 服务类型
     */
    private final Class<?> serviceType;

    /**
     * 服务方法集合
     */
    private final Set<String> allowMethods;

    public RpcServiceProxyInvoker(RpcTransfer transfer, Class<?> serviceType) {
        this.transfer = transfer;
        this.serviceType = serviceType;
        this.allowMethods = Stream.of(serviceType.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!allowMethods.contains(method.getName())) {
            return method.invoke(this, args);
        }

        // 1. 编码
        List<String> paramTypes = Arrays.stream(method.getParameterTypes()).map(Class::getName).toList();
        RpcRequestBody resBody = new RpcRequestBody();
        resBody.setInterfaceName(serviceType.getName());
        resBody.setMethodName(method.getName());
        resBody.setParams(args);
        resBody.setParamTypes(paramTypes);

        RpcRequest request = new RpcRequest();
        request.setHeader("version=1");
        request.setBody(resBody);

        // 2. 传输
        RpcResponse response = transfer.sendRequest(request);

        // 3. 解码
        if (response == null) {
            throw new Exception("RPC invoke failed");
        }
        String header = response.getHeader();
        RpcResponseBody respBody = response.getBody();
        if ("version=1".equals(header)) {
            return respBody.getReturnObject();
        }
        throw new Exception("RPC version mismatches");
    }
}
