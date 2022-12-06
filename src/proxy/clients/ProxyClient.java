package proxy.clients;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ProxyClient implements Client{
    private final ClientType type = ClientType.CLIENT;
    private SocketChannel clientChannel;
    private SocketChannel targetChannel;

    public void addClientChannel(SocketChannel channel){
        this.clientChannel = channel;
    }

    public void addTargetChannel(SocketChannel channel){
        this.targetChannel = channel;
    }

    @Override
    public void receive() {
        ByteBuffer.allocate(150);
    }

    @Override
    public void respond() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    @Override
    public SocketChannel getTargetChannel() {
        return targetChannel;
    }

    @Override
    public ClientType getType() {
        return  type;
    }


}
