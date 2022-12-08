package proxy;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class ProxyClient{ //tunnel between client and target
    ByteBuffer input;
    ByteBuffer output;
    SelectionKey target;
    Status status = Status.DISCONNECTED;
}
