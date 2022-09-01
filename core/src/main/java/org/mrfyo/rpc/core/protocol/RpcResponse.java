package org.mrfyo.rpc.core.protocol;

import org.mrfyo.rpc.core.protocol.RpcResponseBody;

import java.io.Serializable;

/**
 * RPC 响应对象
 *
 * @author fengyong
 */
public class RpcResponse implements Serializable {
    /**
     * 请求头
     */
    private String header;

    /**
     * 请求体
     */
    private RpcResponseBody body;


    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public RpcResponseBody getBody() {
        return body;
    }

    public void setBody(RpcResponseBody body) {
        this.body = body;
    }
}
