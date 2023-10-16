import jdk.nashorn.internal.objects.Global;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class GUDPSocketback implements GUDPSocketAPI {
    DatagramSocket datagramSocket;
    private Queue<GUDPPacket> sendBuffer = new LinkedList<>();
    private InetSocketAddress ACKdestSocketAddresses;
    private Queue<GUDPPacket> sentNotACK = new LinkedList<>();
    private PriorityQueue<Integer> ACKed = new PriorityQueue<Integer>();
    private final int senderWindow = 1;

    public GUDPSocketback(DatagramSocket socket) {
        datagramSocket = socket;
    }



    public void send_packet(GUDPPacket gudppacket) throws IOException{
        DatagramPacket udppacket = gudppacket.pack();
        datagramSocket.send(udppacket);
    }

    public void send(DatagramPacket packet) throws IOException {
        GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
//        DatagramPacket udppacket = gudppacket.pack();
        sendBuffer.add(gudppacket);

//        datagramSocket.send(udppacket);
    }

    public void receive(DatagramPacket packet) throws IOException {
        byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
        DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(udppacket);
        GUDPPacket gudppacket = GUDPPacket.unpack(udppacket);
        gudppacket.decapsulate(packet);

        System.out.println("Received packet type: " + gudppacket.getType());
        System.out.println("Received packet seqNo: " + gudppacket.getSeqno());
        System.out.println("Received packet from: " + gudppacket.getSocketAddress().getHostString());
        if (gudppacket.getType() != GUDPPacket.TYPE_ACK){
//            String ip = gudppacket.getSocketAddress().getHostString();
//            int port = gudppacket.getSocketAddress().getPort();
            ACKdestSocketAddresses = gudppacket.getSocketAddress();
            System.out.println("ACK sent back to " + ACKdestSocketAddresses.getHostString() + ":" + ACKdestSocketAddresses.getPort());
            int ACKNum = gudppacket.getSeqno() + 1;
            GUDPPacket ackGudpPacket = BuildACKPacket(ACKNum, ACKdestSocketAddresses);
            send_packet(ackGudpPacket);
        }
        else {
            ACKed.add(gudppacket.getSeqno());
            System.out.println("In receieve(), Sender received ACKnum is " + gudppacket.getSeqno());
        }

    }

    private GUDPPacket BuildACKPacket(int data, InetSocketAddress ackDest) throws IOException{
        byte[] ackdata = toHH(data);
        DatagramPacket packet = new DatagramPacket(ackdata, ackdata.length, ackDest);
        GUDPPacket ackGudppacket = GUDPPacket.encapsulate(packet);
        ackGudppacket.setType(GUDPPacket.TYPE_ACK);
        ackGudppacket.setSeqno(data);
        return ackGudppacket;
    }

    public static byte[] toHH(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    public static int toInt(byte[] b){
        int res = 0;
        for(int i=0;i<b.length;i++){
            res += (b[i] & 0xff) << ((3-i)*8);
        }
        return res;
    }

    public void finish() throws IOException {

        Timer timer = new Timer();
        Thread timerThread = new Thread(timer);
        timerThread.start();

        ACKReceiver ackReceiver = new ACKReceiver();
        Thread ackReceiverThread = new Thread(ackReceiver);
        ackReceiverThread.start();

        PacketSender packetSender = new PacketSender();
        Thread packetSenderThread = new Thread(packetSender);
        packetSenderThread.start();
//        int sequenceNum = 1;
//        boolean start = false;
//        while (!sendBuffer.isEmpty()){
//            if(sentNotACK.size() >= senderWindow)
//                continue;
//            GUDPPacket dataPacket = sendBuffer.poll();
//            if (!start){
//                start = true;
//                dataPacket.setType(GUDPPacket.TYPE_BSN);
//            }
//            dataPacket.setSeqno(sequenceNum);
//            sequenceNum += 1;
//            sentNotACK.add(dataPacket);
//            send_packet(dataPacket);
//        }
    }
    public void close() throws IOException {
//        End end = new End();
//        Thread endThread = new Thread(end);
//        endThread.start();
    }

    private class End implements Runnable{

        @Override
        public void run() {
            while (true){
                if (sentNotACK.isEmpty()){
                    break;
                }
                System.out.println("sentNotACK size is " + sentNotACK.size());
            }
        }
    }



    private class PacketSender implements Runnable{


        private GUDPPacket BuildBSNPacket(int data, InetSocketAddress ackDest) throws IOException{

            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putInt(3);
            byteBuffer.rewind();
            byte[] buf = new byte[4];
            byteBuffer.get(buf, 0, 4);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, ackDest);
            GUDPPacket ackGudppacket = GUDPPacket.encapsulate(packet);
            ackGudppacket.setType(GUDPPacket.TYPE_BSN);
            ackGudppacket.setVersion(GUDPPacket.GUDP_VERSION);
            ackGudppacket.setSeqno(data);
            return ackGudppacket;
//            byteBuffer.order(ByteOrder.BIG_ENDIAN);
//            byteBuffer.put((byte) 10);
//            GUDPPacket ackGudppacket = new GUDPPacket(byteBuffer);
//            ackGudppacket.setType(GUDPPacket.TYPE_BSN);
//            ackGudppacket.setVersion(GUDPPacket.GUDP_VERSION);
//            ackGudppacket.setSeqno(data);
//            ackGudppacket.setSocketAddress(ackDest);
//            return ackGudppacket;
//            byte[] bsndata = toHH(10);
//            DatagramPacket packet = new DatagramPacket(bsndata, bsndata.length, ackDest);
//            GUDPPacket ackGudppacket = GUDPPacket.encapsulate(packet);
//            ackGudppacket.setType(GUDPPacket.TYPE_BSN);
//            ackGudppacket.setVersion(GUDPPacket.GUDP_VERSION);
//            ackGudppacket.setSeqno(data);
//            return ackGudppacket;
        }

        @Override
        public void run() {
            try {
                int sequenceNum = 1;
                boolean start = false;

                GUDPPacket bsnPacket = BuildBSNPacket(sequenceNum, sendBuffer.peek().getSocketAddress());
                send_packet(bsnPacket);
                while (!sentNotACK.isEmpty()){
                    System.out.println("wait BSN ACK");
                }
                sequenceNum += 1;
                start = true;
                while (!sendBuffer.isEmpty()){
                    System.out.println("PacketSender Running!");
                    if(sentNotACK.size() >= senderWindow)
                        continue;
                    GUDPPacket dataPacket = sendBuffer.poll();
                    if (!start){
                        start = true;
                        dataPacket.setType(GUDPPacket.TYPE_BSN);
                    }
                    dataPacket.setSeqno(sequenceNum);
                    sequenceNum += 1;
                    sentNotACK.add(dataPacket);
//                    resetTimer();
                    send_packet(dataPacket);
                }
            } catch (IOException e){
                System.err.println("Exception in PacketSender");
                System.exit(0);
            }
        }
    }

    private class ACKReceiver implements Runnable{

//        private DatagramSocket ackReceiveSocket = new DatagramSocket(datagramSocket.getLocalPort());
//        private GUDPSocket receiveGUDPSocket = new GUDPSocket(ackReceiveSocket);

        public ACKReceiver() throws SocketException {
        }

        @Override
        public void run() {
            while(true){
                System.out.println("ACKReceiver Running!");
                try {
                    byte[] buf = new byte[4];
                    DatagramPacket ackpacket = new DatagramPacket(buf, 4);
                    receive(ackpacket);

                    byte[] recACKdata = ackpacket.getData();
//                    ACKed.add(toInt(ackpacket.getData()));
//                    ACKed.add(toInt(ackpacket.getData()));

                    System.out.println("Before receieve ACK");
                    System.out.println("senNotACK size is " + sentNotACK.size());
                    System.out.println("ACKed size is " + ACKed.size());
                    System.out.println("senNotACK head is " + sentNotACK.peek().getSeqno());
                    System.out.println("ACKed head is " + ACKed.peek());


                    while (!sentNotACK.isEmpty()){
                        if (ACKed.isEmpty())
                            break;
                        GUDPPacket checkPacket = sentNotACK.peek();
                        int ackNum = ACKed.peek();
                        if (ackNum == checkPacket.getSeqno() + 1){
                            sentNotACK.poll();
                            ACKed.poll();
                        }
                        else if(ackNum < checkPacket.getSeqno() + 1){
                            ACKed.poll();
                        }
                        else {
                            send_packet(checkPacket);
                            break;
                        }
                    }
                    System.out.println("After receieved ACK");
                    System.out.println("senNotACK size is " + sentNotACK.size());
                    System.out.println("ACKed size is " + ACKed.size());
                } catch (Exception e){
                    System.err.println("Exception in ACKReceiver");
                    System.exit(0);
                }
            }
        }
    }

    private class Timer implements Runnable{

        private int time = 5000;

        public void resetTimer(){
            this.time = 5000;
        }
        @Override
        public void run() {
            while (true){
                if (sentNotACK.isEmpty()){
                    resetTimer();
                    continue;
                }
                time -= 1;
                if (time == 0){
                    System.out.println("Time out! Resend!");
                    try {
                        send_packet(sentNotACK.peek());
                    } catch (IOException e) {
                        System.out.println("resend packet error in Timer");
                    }
                    resetTimer();
                }
            }
        }
    }
}

