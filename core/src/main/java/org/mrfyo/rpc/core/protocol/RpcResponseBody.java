package org.mrfyo.rpc.core.protocol;

import java.io.Serializable;

/**
 * RPC 响应体
 *
 * @author fengyong
 */
public class RpcResponseBody implements Serializable {
    /**
     * 返回值
     */
    private Object returnObject;

    private String returnType;


    public Object getReturnObject() {
        return returnObject;
    }

    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
}
