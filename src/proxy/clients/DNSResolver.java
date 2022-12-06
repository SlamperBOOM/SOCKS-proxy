package proxy.clients;

import java.nio.channels.SocketChannel;

public class DNSResolver implements Client{
    private final ClientType type = ClientType.DNS;
    private SocketChannel dnsChannel;

    public DNSResolver(){
        //создавать сокет для dns resolver
    }

    @Override
    public void receive() {

    }

    @Override
    public void respond() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public SocketChannel getClientChannel() {
        return null;
    }

    @Override
    public SocketChannel getTargetChannel() {
        return dnsChannel;
    }

    @Override
    public ClientType getType() {
        return type;
    }
}
