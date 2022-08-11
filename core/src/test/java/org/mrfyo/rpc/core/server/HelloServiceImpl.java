package org.mrfyo.rpc.core.server;

import org.mrfyo.rpc.core.service.HelloService;

public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "hello " + name;
    }
}
