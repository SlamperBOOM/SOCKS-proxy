package proxy;

import proxy.clients.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
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
        clients = new Clients(this);
    }

    @Override
    public void run(){
        while (isRunning){
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        //создаем клиента, помещаем в список и селектор
                        SocketChannel clientChannel = serverChannel.accept();
                        ProxyClient client = new ProxyClient(clients);
                        clientChannel.register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
                        clientChannel.configureBlocking(false);
                        client.addClientChannel(clientChannel);
                        clients.addClient(client);
                    } else if (key.isReadable()) {
                        //определяем от кого пришло сообщение и делегируем обработку нужному клиенту
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ProxyClient client = clients.findClient(clientChannel);
                        client.socketReady(clientChannel);
                    } else {

                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void addTargetToSelector(SocketChannel channel){
        try {
            channel.register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    public void deleteChannel(SocketChannel channel){
        for(SelectionKey key : selector.keys()){
            if(key.channel().equals(channel)){
                key.cancel();
                break;
            }
        }
    }

    public void setStopped(){
        isRunning = false;
    }
}
