package proxy.clients;

import java.nio.channels.SocketChannel;

public interface Client {
    void receive();
    void respond();
    void disconnect();
    SocketChannel getClientChannel();
    SocketChannel getTargetChannel();
    ClientType getType();
}
