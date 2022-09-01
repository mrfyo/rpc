package org.mrfyo.rpc.core.server;

import java.io.Closeable;
import java.io.IOException;

public interface RpcServer extends Closeable, Runnable {

    <T> void register(Class<T> serviceType, T service);

    <T> void unregister(Class<T> serviceType);

}
