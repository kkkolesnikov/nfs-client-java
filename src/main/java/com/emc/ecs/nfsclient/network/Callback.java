package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.RpcException;

public interface Callback<T> {
    void invoke(T t) throws RpcException;
}
