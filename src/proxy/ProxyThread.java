package proxy;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;

public class ProxyThread extends Thread{
    private final int BUFSIZ = 8196;
    private boolean isRunning = true;
    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private final InetAddress selfAddress;

    public ProxyThread(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();

        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        selfAddress = InetAddress.getLocalHost(); //узнаем собственный ip
    }

    @Override
    public void run(){
        while (isRunning){
            try {
                selector.select();
                if(!selector.isOpen()){
                    continue;
                }
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = ((ServerSocketChannel)key.channel()).accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        read(key);
                    }else if(key.isConnectable()){
                        connectToHost(key);
                    }else if(key.isWritable()){
                        write(key);
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void read(SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();
        ProxyClient client = (ProxyClient)key.attachment();
        if(client == null){
            key.attach(client = new ProxyClient());
            client.input = ByteBuffer.allocate(BUFSIZ);
        }
        try {
            if (channel.read(client.input) < 1) {//smth went wrong
                System.out.println("Smth went wrong");
                close(key);
            } else if(client.status == Status.DISCONNECTED){ //запрос подключение к прокси
                System.out.println("Connection to proxy");
                connectToProxy(key);
            } else if(client.status == Status.CONNECTED){//запрос на создание тоннеля
                System.out.println("Requesting connection to host");
                if(client.input.array()[0] == 0x05){
                    if(client.input.array()[1] == 0x01) {//connect command
                        if (client.input.array()[3] == 0x01) {//IPv4 address
                            String address;
                            try {
                                address = InetAddress.getByAddress(
                                        Arrays.copyOfRange(client.input.array(), 5, 5 + 4)
                                ).getHostAddress();
                            } catch (UnknownHostException e) {//cannot reach remote host
                                e.printStackTrace();
                                byte[] answer = createAnswer(0);
                                answer[1] = 0x04;
                                try {
                                    channel.write(ByteBuffer.wrap(answer));
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                System.out.println("Cannot reach host");
                                close(key);
                                return;
                            }

                            short port = ByteBuffer.wrap(
                                    Arrays.copyOfRange(client.input.array(), 5 + 4, 5 + 4 + 2)
                            ).getShort();

                            SocketAddress socketAddress = InetSocketAddress.createUnresolved(address, port);
                            SocketChannel targetChannel;
                            try {
                                targetChannel = SocketChannel.open();
                                targetChannel.configureBlocking(false);
                                targetChannel.connect(socketAddress);
                                SelectionKey targetKey = targetChannel.register(selector, SelectionKey.OP_CONNECT);
                                ((ProxyClient)key.attachment()).target = targetKey;
                                key.interestOps(0);
                                ProxyClient target = new ProxyClient();
                                target.target = key;
                                targetKey.attach(target);
                                client.input.clear();
                                System.out.println("Requested connection through ip: " + address);
                            } catch (IOException e) {//connection refused
                                e.printStackTrace();
                                byte[] answer = createAnswer(0);
                                answer[1] = 0x05;
                                try {
                                    channel.write(ByteBuffer.wrap(answer));
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                System.out.println("Connection refused by host");
                                close(key);
                            }
                        } else if (client.input.array()[3] == 0x03) {//domain name
                            byte nameLength = client.input.array()[4];
                            String name = new String(Arrays.copyOfRange(client.input.array(), 5, 5 + nameLength));
                            String address;
                            try {
                                address = InetAddress.getByName(name).getHostAddress();
                            }catch (UnknownHostException e){//name unresolved
                                e.printStackTrace();
                                byte[] answer = createAnswer(0);
                                answer[1] = 0x03;
                                try {
                                    channel.write(ByteBuffer.wrap(answer));
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                System.out.println("Name unresolved");
                                close(key);
                                return;
                            }

                            short port = ByteBuffer.wrap(
                                    Arrays.copyOfRange(client.input.array(), 5 + nameLength, 5 + nameLength + 2)
                            ).getShort();

                            SocketAddress socketAddress = InetSocketAddress.createUnresolved(address, port);
                            SocketChannel targetChannel;
                            try {
                                targetChannel = SocketChannel.open();
                                targetChannel.configureBlocking(false);
                                targetChannel.connect(socketAddress);
                                SelectionKey targetKey = targetChannel.register(selector, SelectionKey.OP_CONNECT);
                                ((ProxyClient)key.attachment()).target = targetKey;
                                key.interestOps(0);
                                ProxyClient target = new ProxyClient();
                                target.target = key;
                                targetKey.attach(target);
                                client.input.clear();
                                System.out.println("Requested connection through domain " + name + ", ip: " + address);
                            } catch (IOException e) {//connection refused
                                e.printStackTrace();
                                byte[] answer = createAnswer(0);
                                answer[1] = 0x05;
                                try {
                                    channel.write(ByteBuffer.wrap(answer));
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                System.out.println("Connection refused by host");
                                close(key);
                            }
                        } else {//IPv6, not supported
                            //посылаем ответ, что тип адреса не поддерживается
                            byte[] answer = createAnswer(0);
                            answer[1] = 0x08;
                            try {
                                channel.write(ByteBuffer.wrap(answer));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Requested connection through IPv6");
                            close(key);
                        }
                    }else{
                        //посылаем ответ, что это неподдерживаемая команда
                        byte[] answer = createAnswer(0);
                        answer[1] = 0x02;
                        try {
                            channel.write(ByteBuffer.wrap(answer));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Command not supported");
                        close(key);
                    }
                }else{
                    //посылаем ответ, что версия протокола не верна
                    byte[] answer = createAnswer(0);
                    answer[1] = 0x07;
                    try {
                        channel.write(ByteBuffer.wrap(answer));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("SOCKS version mismatch");
                    close(key);
                }
            }else if(client.status == Status.TRANSLATING){
                client.target.interestOps(client.target.interestOps() | SelectionKey.OP_WRITE);
                key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
                client.input.flip();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void connectToProxy(SelectionKey key){
        try {
            if(((ProxyClient)key.attachment()).input.array()[0] == 0x05){
                boolean isNoAuthNormal = false;
                for(int i=2; i < ((ProxyClient)key.attachment()).input.array()[1] + 2; ++i){
                    if(((ProxyClient)key.attachment()).input.array()[i] == 0x00){ //NoAuthMethod is normal for client
                        ((SocketChannel)key.channel()).write(ByteBuffer.allocate(2)
                                .put((byte) 0x05)
                                .put((byte) 0x00)
                                .flip());
                        isNoAuthNormal = true;
                        ((ProxyClient)key.attachment()).status = Status.CONNECTED;
                        System.out.println("Client connected to proxy");
                        break;
                    }
                }
                if(!isNoAuthNormal){//клиенту не подходит NoAuth метод
                    ((SocketChannel)key.channel()).write(ByteBuffer.allocate(2)
                            .put((byte) 0x05)
                            .put((byte) 0xFF));
                    System.out.println("NoAuth method doesn't requested");
                    close(key);
                }

            }else{//если не совпадает версия SOCKS
                ((SocketChannel)key.channel()).write(ByteBuffer.allocate(2)
                        .put((byte) 0x05)
                        .put((byte) 0xFF));
                System.out.println("SOCKS version mismatch");
                close(key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToHost(SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();
        ProxyClient client = (ProxyClient) key.attachment();

        try {
            channel.finishConnect();
        }catch (IOException e){
            e.printStackTrace();
        }
        client.input = ByteBuffer.allocate(BUFSIZ);
        client.input.put(createAnswer(channel.socket().getPort()));
        client.output = ((ProxyClient)client.target.attachment()).input;
        ((ProxyClient)client.target.attachment()).output = client.input;
        ((ProxyClient)client.target.attachment()).status = Status.TRANSLATING;
        client.target.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        key.interestOps(0);

        System.out.println("Connected to host");
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ProxyClient client = (ProxyClient) key.attachment();

        if((channel.write(client.output) == -1)){
            close(key);
        }else if(client.output.remaining() == 0){
            if(client.target == null){
                close(key);
            }else{
                client.output.clear();

                client.target.interestOps(client.target.interestOps() | SelectionKey.OP_READ);
                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                System.out.println("Retranslated traffic");
            }
        }
    }

    private void close(SelectionKey key){
        try {
            key.channel().close();
            if(key.attachment() != null && ((ProxyClient) key.attachment()).target != null) {
                ((ProxyClient) key.attachment()).target.channel().close();
                SelectionKey targetKey = ((ProxyClient) key.attachment()).target;
                if (targetKey != null) {
                    ((ProxyClient) targetKey.attachment()).target = null;
                    if ((targetKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                        ((ProxyClient) targetKey.attachment()).output.flip();
                    }
                    targetKey.interestOps(SelectionKey.OP_WRITE);
                }
            }
            System.out.println("Connection closed");
        }catch (IOException e){
            e.printStackTrace();
        }
        key.cancel();
    }

    private byte[] createAnswer(int port){
        byte[] answer = new byte[3/*статусные байты*/+1+4/*IP4 address*/+2/*порт*/];
        answer[0] = 0x05; answer[1] = 0x00; answer[2] = 0x00;
        answer[3] = 0x01;
        byte[] byteAddress = selfAddress.getAddress();
        answer[4] = byteAddress[0]; answer[5] = byteAddress[1];
        answer[6] = byteAddress[2]; answer[7] = byteAddress[3];
        byte[] bytePort = ByteBuffer.allocate(2).putShort((short)port).array();
        answer[8] = bytePort[0]; answer[9] = bytePort[1];
        return answer;
    }

    public void setStopped(){
        isRunning = false;
        try {
            selector.close();
            serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
