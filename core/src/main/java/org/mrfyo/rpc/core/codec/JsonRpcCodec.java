package org.mrfyo.rpc.core.codec;

import com.alibaba.fastjson.JSON;


public class JsonRpcCodec implements RpcCodec {
    @Override
    public byte[] encode(Object obj) {
        return JSON.toJSONBytes(obj);
    }

    @Override
    public <T> T decode(byte[] b, Class<T> type) {
        return JSON.parseObject(b, type);
    }
}
