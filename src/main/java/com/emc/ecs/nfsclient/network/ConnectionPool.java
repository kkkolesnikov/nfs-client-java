package com.emc.ecs.nfsclient.network;

import java.io.Closeable;
import java.io.IOException;

public interface ConnectionPool extends Closeable  {
    Connection getConnection();
    void dropConnection(Connection connection);

    @Override
    void close() throws IOException;
}
