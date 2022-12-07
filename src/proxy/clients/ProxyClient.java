package proxy.clients;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ProxyClient{
    private final ClientType type = ClientType.CLIENT;
    private SocketChannel clientChannel;
    private SocketChannel targetChannel;

    public void addClientChannel(SocketChannel channel){
        this.clientChannel = channel;
    }

    public void addTargetChannel(SocketChannel channel){
        this.targetChannel = channel;
    }

    public void receive() {
        ByteBuffer.allocate(150);
    }

    public void respond() {

    }

    public void disconnect() {

    }

    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    public SocketChannel getTargetChannel() {
        return targetChannel;
    }

    public ClientType getType() {
        return  type;
    }


}
