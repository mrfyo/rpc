package org.mrfyo.rpc.core.invoker;

import org.mrfyo.rpc.core.protocol.RpcRequestBody;
import org.mrfyo.rpc.core.protocol.RpcResponseBody;

import java.lang.reflect.Method;
import java.util.Map;

public class SimpleRpcInvoker implements RpcInvoker {

    private final Map<String, Object> servicePool;

    public SimpleRpcInvoker(Map<String, Object> servicePool) {
        this.servicePool = servicePool;
    }

    @Override
    public RpcResponseBody invoke(RpcRequestBody reqBody) {
        Object result = execute(reqBody);
        RpcResponseBody respBody = new RpcResponseBody();
        respBody.setReturnObject(result);
        respBody.setReturnType(result == null ? "" : result.getClass().getName());
        return respBody;
    }

    private Object execute(RpcRequestBody reqBody) {
        Object result = null;
        try {
            Object service = servicePool.get(reqBody.getInterfaceName());

            int paramSize = reqBody.getParamTypes().size();
            Class<?>[] paramTypes = new Class[paramSize];
            for (int i = 0; i < paramSize; i++) {
                paramTypes[i] = Class.forName(reqBody.getParamTypes().get(i));
            }

            Method method = service.getClass().getDeclaredMethod(reqBody.getMethodName(), paramTypes);
            result = method.invoke(service, reqBody.getParams());
        } catch (Exception e) {
            System.err.println("service invoke failed.");
        }
        return result;
    }
}
