package org.mrfyo.rpc.core.client;

import org.mrfyo.rpc.core.protocol.RpcRequest;
import org.mrfyo.rpc.core.protocol.RpcResponse;

import java.io.Closeable;

public interface Transfer extends Closeable {

    RpcResponse sendRequest(RpcRequest request);
}
