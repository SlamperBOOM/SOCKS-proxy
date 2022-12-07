package proxy.clients;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Clients {
    private List<ProxyClient> clients;

    public Clients(){
        clients = new ArrayList<>();
    }

    public void addClient(ProxyClient client){
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

    public void closeAll(){

    }

    protected void resolveDomain(String domain){

    }
}
