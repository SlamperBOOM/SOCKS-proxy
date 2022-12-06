package proxy;

import proxy.clients.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ProxyThread extends Thread{
    private boolean isRunning = true;
    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private final Clients clients;

    public ProxyThread(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();

        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        clients = new Clients();

        DNSResolver resolver = new DNSResolver();
        resolver.getTargetChannel().register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
        clients.addClient(resolver);
    }

    @Override
    public void run(){
        while (isRunning){
            try {
                selector.select();

                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        //создаем клиента, помещаем в список и селектор
                        SocketChannel clientChannel = serverChannel.accept();
                        ProxyClient client = new ProxyClient();
                        clientChannel.register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
                        client.addClientChannel(clientChannel);
                    } else if (key.isReadable()) {
                        //определяем от кого пришло сообщение и делегируем обработку нужному клиенту
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        Client client = clients.findClient(clientChannel);
                        if(client.getType() == ClientType.CLIENT){
                            client.receive();
                        }else{

                        }
                    } else {

                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void setStopped(){
        isRunning = false;
    }
}
