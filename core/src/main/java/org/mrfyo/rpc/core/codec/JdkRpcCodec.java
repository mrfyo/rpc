package org.mrfyo.rpc.core.codec;

import java.io.*;

public class JdkRpcCodec implements RpcCodec {

    @Override
    public byte[] encode(Object obj) throws IOException {
        ByteArrayOutputStream bis = new ByteArrayOutputStream(512);
        ObjectOutputStream oos = new ObjectOutputStream(bis);
        oos.writeObject(obj);
        return bis.toByteArray();
    }

    @Override
    public <T> T decode(byte[] b, Class<T> type) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInputStream ois = new ObjectInputStream(bis);
        try {
            return type.cast(ois.readObject());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
