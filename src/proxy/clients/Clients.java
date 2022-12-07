package proxy.clients;

import proxy.ProxyThread;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Clients {
    private final ProxyThread thread;
    private final List<ProxyClient> clients;

    public Clients(ProxyThread thread){
        clients = new ArrayList<>();
        this.thread = thread;
    }

    public void addClient(ProxyClient client) {
        clients.add(client);
    }

    public ProxyClient findClient(SocketChannel channel){
        for(ProxyClient client : clients){
            if(client.getClientChannel().equals(channel) || client.getTargetChannel().equals(channel)){
                return client;
            }
        }
        return null;
    }

    protected void addChannelToSelector(SocketChannel channel){
        thread.addTargetToSelector(channel);
    }

    protected void deleteChannels(ProxyClient client){
        thread.deleteChannel(client.getClientChannel());
        thread.deleteChannel(client.getTargetChannel());
    }

    public void closeAll(){

    }
}
