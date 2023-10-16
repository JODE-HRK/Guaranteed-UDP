import java.lang.reflect.Array;
import java.net.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class GUDPSocket implements GUDPSocketAPI {
    DatagramSocket datagramSocket;

    ACKMonitor ackMonitor = new ACKMonitor();
    Sender sender = new Sender();
    ProcessACK processACK = new ProcessACK();
    Timer timer = new Timer();
    Thread timerThread = new Thread(timer);
    Thread senderThread = new Thread(sender);
    Thread ackMonitorThread = new Thread(ackMonitor);
    Thread processACKThread = new Thread(processACK);

    public GUDPSocket(DatagramSocket socket) {
        datagramSocket = socket;
    }

    public void send_Packet(GUDPPacket gudpPacket) throws IOException {
        DatagramPacket udppacket = gudpPacket.pack();
        datagramSocket.send(udppacket);
    }

    public Queue<GUDPPacket> gudpDataBuffer = new LinkedList<>();

    public void send(DatagramPacket packet) throws IOException {
        GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
        gudpDataBuffer.add(gudppacket);
    }

    public int window_Size = 1;
    public Queue<GUDPPacket> SentNotACK = new LinkedList<>();

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;
        }
        return value;
    }

    void BuildandSendACKPacket(int seqNo, String destinationIP, int destination_port) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(GUDPPacket.HEADER_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);
        GUDPPacket ackPacket = new GUDPPacket(buffer);
        ackPacket.setType(GUDPPacket.TYPE_ACK);
        ackPacket.setVersion(GUDPPacket.GUDP_VERSION);
        ackPacket.setSeqno(seqNo);
        ackPacket.setPayloadLength(0);
        InetSocketAddress destination = new InetSocketAddress(destinationIP,destination_port);
        ackPacket.setSocketAddress(destination);
        send_Packet(ackPacket);
    }

    static Comparator<GUDPPacket> cmp = new Comparator<GUDPPacket>() {
        public int compare(GUDPPacket p1, GUDPPacket p2) {
            return p1.getSeqno() - p2.getSeqno();
//            return e2 - e1;
        }
    };

    PriorityQueue <Integer> NumberReceievedButUnconfirmedACK = new PriorityQueue<Integer>();
    Queue<GUDPPacket> ReceievedButUnconfirmedACK = new PriorityQueue<>(cmp);
//    PriorityQueue<Integer> NumberReceievedButUnconfirmedACK = new PriorityQueue<cmpint>;

    public void receive(DatagramPacket packet) throws IOException {
        byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
        DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(udppacket);
        GUDPPacket gudppacket = GUDPPacket.unpack(udppacket);
        gudppacket.decapsulate(packet);
//        System.out.println("What I received:" + byteArrayToInt(gudppacket.getBytes()));
        System.out.println("REC Yes!");

        if (gudppacket.getType() != GUDPPacket.TYPE_ACK){
            int seqNo = gudppacket.getSeqno();
            String destinationIP = packet.getAddress().getHostAddress();
            int destinationPort = 10000;
            System.out.println("Sent ACK Packet!");
            BuildandSendACKPacket(seqNo + 1, destinationIP, destinationPort);
        }
    }


    public class Timer implements Runnable{

        public int standardTime;
        public int count;

        public Timer(){
            standardTime = 5000;
            count = 0;
        }

        public void resetTimer(){
            count = 0;
        }

        @Override
        public void run() {
            while (true){
                while (count < standardTime)
                    count++;
                if (!SentNotACK.isEmpty()){
                    try {
                        send_Packet(SentNotACK.peek());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    count = 0;
                }
            }
        }
    }

    public class ProcessACK implements Runnable{
        // To remove ACKed Packet
        public ProcessACK(){
            int time = 0;
        }

        @Override
        public void run(){
            System.out.println("ProcessACK start!");
            while (true){
                if(ReceievedButUnconfirmedACK.isEmpty())
                    continue;
                while (!SentNotACK.isEmpty()){
                    if (NumberReceievedButUnconfirmedACK.isEmpty())
                        continue;
                    int received_num = NumberReceievedButUnconfirmedACK.peek();
                    GUDPPacket topSNA = SentNotACK.peek();  // num in Seq
                    byte[] dataACK = new byte[GUDPPacket.MAX_DATA_LEN];
                    DatagramPacket dp = new DatagramPacket(dataACK, dataACK.length);
                    if (received_num - 1 == topSNA.getSeqno()){
                        NumberReceievedButUnconfirmedACK.poll();
                        SentNotACK.poll();
                        window_Size = Math.max(window_Size + 1 ,3);
                        timer.resetTimer();
                    }
                    else {
                        window_Size = window_Size - 1;
                        if (window_Size == 0)
                            window_Size = 1;
                    }
                }
            }
        }
    }

    public class ACKMonitor implements Runnable{
        // To receive ACK packet
        public ACKMonitor(){
        }

        @Override
        public void run() {
            System.out.println("ACKMonitor start!");
            DatagramSocket ACKdatagramSocket;
            try {
                ACKdatagramSocket = new DatagramSocket(10000);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            GUDPSocket gudpSocket = new GUDPSocket(ACKdatagramSocket);
            while (true){
                byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
                DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
                try {
                    gudpSocket.receive(udppacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("ACKmonitor working!!");
                GUDPPacket gudppacket = null;
                try {
                    gudppacket = GUDPPacket.unpack(udppacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    gudppacket.decapsulate(udppacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                int received_seq = gudppacket.getSeqno();
                NumberReceievedButUnconfirmedACK.add(received_seq);
                if (gudppacket.getType() == GUDPPacket.TYPE_BSN)
                    startData = true;
            }
        }
    }

    public int last_seq;

    public class Sender implements Runnable{

        public Sender(){
        }

        @Override
        public void run() {
            System.out.println("Sender Start!");

//            System.out.println("in run()");
            while (!gudpDataBuffer.isEmpty()){
                if(!startData){
                    System.out.println(startData);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }
                if(SentNotACK.size() < window_Size){
                    GUDPPacket gudpDataPacket = gudpDataBuffer.poll();
                    gudpDataPacket.setType(GUDPPacket.TYPE_DATA);
                    last_seq += 1;
                    gudpDataPacket.setSeqno(last_seq);
                    SentNotACK.add(gudpDataPacket);
                    try {
                        send_Packet(gudpDataPacket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
//                SentNotACK.poll(); // give up ACK
            }
        }
    }

    public boolean startData = false;

    public void finish() throws IOException {
        // To receive ACK packet
        System.out.println("Start?");

        this.ackMonitorThread.start();
        processACKThread.start();
        senderThread.start();
        timerThread.start();
        System.out.println("Start!");
        System.out.println("How many data in buffer?    " + gudpDataBuffer.size());
        System.out.println("How many data in SentNotACK?    " + SentNotACK.size());

        GUDPPacket bsnPacket = gudpDataBuffer.poll();
        bsnPacket.setType(GUDPPacket.TYPE_BSN);
        SentNotACK.add(bsnPacket);
        send_Packet(bsnPacket);

        // wait until bsn packet sent and ACKed
        while (!SentNotACK.isEmpty()){

        }

        while (true){
            if (!startData)
                continue;
//            System.out.println("It's time to sent " + last_seq);
            System.out.println("SentNotACK Size is " + SentNotACK.size());
            System.out.println("ReceievedButUnconfirmedACK Size is " + NumberReceievedButUnconfirmedACK.size());
            System.out.println("gudpBuffer Size is " + gudpDataBuffer.size());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(SentNotACK.isEmpty() && ReceievedButUnconfirmedACK.isEmpty() && gudpDataBuffer.isEmpty()){
//                close();
                break;
            }
        }
    }
    public void close() throws IOException {
        System.out.println("Close and End!");
        startData = false;
    }
}

