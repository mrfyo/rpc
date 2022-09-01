package org.mrfyo.rpc.core.codec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;


public class KryoRpcCodec implements RpcCodec {

    private final Pool<Kryo> kryoPool;

    public KryoRpcCodec(Pool<Kryo> kryoPool) {
        this.kryoPool = kryoPool;
    }

    public KryoRpcCodec(int cap) {
        this.kryoPool = new Pool<>(true, false, cap) {
            @Override
            protected Kryo create() {
                Kryo kryo = new Kryo();
                kryo.setRegistrationRequired(false);
                return kryo;
            }
        };
    }


    @Override
    public byte[] encode(Object obj) {
        Kryo kryo = kryoPool.obtain();
        try {
            Output output = new Output(512);
            kryo.writeClassAndObject(output, obj);
            return output.toBytes();
        } finally {
            kryoPool.free(kryo);
        }

    }

    @Override
    public <T> T decode(byte[] b, Class<T> type) {
        Kryo kryo = kryoPool.obtain();
        try {
            Input input = new Input(b);
            Object obj = kryo.readClassAndObject(input);
            return type.cast(obj);
        } finally {
            kryoPool.free(kryo);
        }
    }
}
