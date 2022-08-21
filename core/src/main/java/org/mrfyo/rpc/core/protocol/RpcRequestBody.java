package org.mrfyo.rpc.core.protocol;

import java.io.Serializable;
import java.util.List;

/**
 * RPC 请求体
 *
 * @author fengyong
 */
public class RpcRequestBody implements Serializable {
    /**
     * 接口名
     */
    private String interfaceName;
    /**
     * 方法名
     */
    private String methodName;
    /**
     * 参数列表
     */
    private Object[] params;
    /**
     * 参数类型列表
     */
    private List<String> paramTypes;


    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public List<String> getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(List<String> paramTypes) {
        this.paramTypes = paramTypes;
    }
}
