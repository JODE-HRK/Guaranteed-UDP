import java.net.DatagramPacket;
import java.io.IOException;

public interface GUDPSocketAPI {

    public void send(DatagramPacket packet) throws IOException;
//    The send() method takes a DatagramPacket and encapsulates it
//    in a GUDP packet before sending it to its destination endpoint.
//    The receive() method receives a packet from a remote endpoint.
//    The received packet is decapsulated and placed in the DatagramPacket
//    before passing it as a parameter to receive().

    public void receive(DatagramPacket packet) throws IOException;
    public void finish() throws IOException;
    //    The finish() method is called by a GUDP sender to indicate
//    that it has sent the last datagram in a communication.
//    If the finish method returns normally, it means that
//    all previous datagrams have been delivered successfully.
//    If the delivery fails, the finish() method throws an IOException.
    public void close() throws IOException;
//    The close() method deletes the socket and frees up any resources used by the socket.
}

