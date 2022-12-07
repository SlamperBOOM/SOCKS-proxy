package proxy.clients;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class ProxyClient{
    private final Clients clients;
    private SocketChannel clientChannel;
    private SocketChannel targetChannel;
    private Status status = Status.DISCONNECTED;

    public ProxyClient(Clients clients){
        this.clients = clients;
    }

    public void addClientChannel(SocketChannel channel){
        this.clientChannel = channel;
    }

    public void addTargetChannel(SocketChannel channel){
        this.targetChannel = channel;
    }

    private void receive() {
        if(status == Status.DISCONNECTED) {
            ByteBuffer buffer = ByteBuffer.allocate(300);
            try {
                int readBytes = clientChannel.read(buffer);
                byte[] receiving = Arrays.copyOfRange(buffer.array(), 0, readBytes);
                if(receiving[0] == 0x05){
                    boolean isNoAuthNormal = false;
                    for(int i=2; i< receiving[1]; ++i){
                        if(receiving[i] == 0x00){ //NoAuthMethod
                            clientChannel.write(ByteBuffer.allocate(2)
                                    .put((byte) 0x05)
                                    .put((byte) 0x00));
                            isNoAuthNormal = true;
                            break;
                        }
                    }
                    if(!isNoAuthNormal){
                        clientChannel.write(ByteBuffer.allocate(2)
                                .put((byte) 0x05)
                                .put((byte) 0xFF));
                    }
                }else{

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            status = Status.CONNECTED;
        }else if (status == Status.CONNECTED){
            ByteBuffer buffer = ByteBuffer.allocate(300);
            try {
                int readBytes = clientChannel.read(buffer);
                byte[] receiving = Arrays.copyOfRange(buffer.array(), 0, readBytes);
                if(receiving[0] == 0x05 && receiving[1] == 0x01){
                    if(receiving[3] == 0x01){
                        String address = InetAddress.getByAddress(
                                Arrays.copyOfRange(receiving, 5, 5+4)
                        ).getHostAddress();

                        short port = ByteBuffer.wrap(
                                Arrays.copyOfRange(receiving, 5+4, 5+4+2)
                        ).getShort();

                        SocketAddress address1 = InetSocketAddress.createUnresolved(address, port);

                        targetChannel = SocketChannel.open(address1);
                        clients.addChannelToSelector(targetChannel);
                    }else if(receiving[3] == 0x03){
                        byte nameLength = receiving[4];
                        String name = new String(Arrays.copyOfRange(receiving, 5, 5+nameLength));
                        String address = InetAddress.getByName(name).getHostAddress();

                        short port = ByteBuffer.wrap(
                                Arrays.copyOfRange(receiving, 5+nameLength, 5+nameLength+2)
                        ).getShort();

                        SocketAddress address1 = InetSocketAddress.createUnresolved(address, port);

                        targetChannel = SocketChannel.open(address1);
                        clients.addChannelToSelector(targetChannel);
                    }else{

                    }
                }else{

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (status == Status.TRANSLATING){
            ByteBuffer buffer = ByteBuffer.allocate(16000);
            try {
                int readBytes = clientChannel.read(buffer);
                targetChannel.write(ByteBuffer.allocate(readBytes).put(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void respond() {

    }

    public void socketReady(SocketChannel channel){
        if(channel.equals(clientChannel)){
            receive();
        }else{
            respond();
        }
    }

    public void disconnect() {
        try {
            clients.deleteChannels(this);
            clientChannel.close();
            targetChannel.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    public SocketChannel getTargetChannel() {
        return targetChannel;
    }
}
