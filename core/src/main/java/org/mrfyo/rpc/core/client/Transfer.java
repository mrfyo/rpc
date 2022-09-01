package org.mrfyo.rpc.core.client;

import org.mrfyo.rpc.core.codec.RpcRequest;
import org.mrfyo.rpc.core.codec.RpcResponse;

import java.io.Closeable;

public interface Transfer extends Closeable {

    RpcResponse sendRequest(RpcRequest request);
}
