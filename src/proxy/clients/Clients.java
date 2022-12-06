package proxy.clients;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Clients {
    private List<Client> clients;
    private DNSResolver resolver;

    public Clients(){
        clients = new ArrayList<>();
    }

    public void addResolver(DNSResolver resolver){
        this.resolver = resolver;
    }

    public void addClient(Client client){
        clients.add(client);
    }

    public Client findClient(SocketChannel channel){
        for(Client client : clients){
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
