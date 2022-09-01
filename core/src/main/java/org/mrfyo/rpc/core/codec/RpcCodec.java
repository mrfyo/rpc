package org.mrfyo.rpc.core.codec;


import java.io.IOException;

public interface RpcCodec {

    byte[] encode(Object obj) throws IOException;

    <T> T decode(byte[] b, Class<T> type) throws IOException;

}
