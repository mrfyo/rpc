package org.mrfyo.rpc.core.protocol;


import java.io.Serializable;

/**
 * RPC 请求对象
 *
 * @author fengyong
 */
public class RpcRequest implements Serializable {
    /**
     * 请求头
     */
    private String header;

    /**
     * 请求体
     */
    private RpcRequestBody body;


    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public RpcRequestBody getBody() {
        return body;
    }

    public void setBody(RpcRequestBody body) {
        this.body = body;
    }
}
