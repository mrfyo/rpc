package org.mrfyo.rpc.core.invoker;

import org.mrfyo.rpc.core.protocol.RpcRequestBody;
import org.mrfyo.rpc.core.protocol.RpcResponseBody;

public interface RpcInvoker {

    RpcResponseBody invoke(RpcRequestBody request);

}
