//import java.lang.reflect.Array;
//import java.net.*;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.*;
//
//public class GUDPSocket implements GUDPSocketAPI {
//    DatagramSocket datagramSocket;
//
//    public GUDPSocket(DatagramSocket socket) {
//        datagramSocket = socket;
//    }
//
//    public void send_Packet(GUDPPacket gudpPacket) throws IOException {
//        DatagramPacket udppacket = gudpPacket.pack();
//        datagramSocket.send(udppacket);
//    }
//
//    public Queue<GUDPPacket> gudpDataBuffer = new LinkedList<>();
//
//    public void send(DatagramPacket packet) throws IOException {
//        GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
//        gudpDataBuffer.add(gudppacket);
//    }
//
//    public int window_Size = 1;
//    public Queue<GUDPPacket> SentNotACK = new LinkedList<>();
//
//    public static byte[] intToByteArray(int i) {
//        byte[] result = new byte[4];
//        result[0] = (byte) ((i >> 24) & 0xFF);
//        result[1] = (byte) ((i >> 16) & 0xFF);
//        result[2] = (byte) ((i >> 8) & 0xFF);
//        result[3] = (byte) (i & 0xFF);
//        return result;
//    }
//
//    public static int byteArrayToInt(byte[] bytes) {
//        int value = 0;
//        for (int i = 0; i < 4; i++) {
//            int shift = (4 - 1 - i) * 8;
//            value += (bytes[i] & 0x000000FF) << shift;
//        }
//        return value;
//    }
//
//    void BuildandSendACKPacket(int seqNo, String destinationIP, int destination_port) throws IOException {
//        byte[] databuf = intToByteArray(seqNo);
//        byte[] buf = databuf;
//        System.out.println("What I send in ACK Packet?" + byteArrayToInt(buf));
//        DatagramPacket ackPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName(destinationIP), destination_port);
//        GUDPPacket gudppacket = GUDPPacket.encapsulate(ackPacket);
//        gudppacket.setType(GUDPPacket.TYPE_ACK);
//        send_Packet(gudppacket);
//    }
//
//    static Comparator<GUDPPacket> cmp = new Comparator<GUDPPacket>() {
//        public int compare(GUDPPacket p1, GUDPPacket p2) {
//            return p1.getSeqno() - p2.getSeqno();
////            return e2 - e1;
//        }
//    };
//
//    PriorityQueue <Integer> NumberReceievedButUnconfirmedACK = new PriorityQueue<Integer>( new Comparator<Integer>() {
//
//        @Override
//        public int compare(Integer o1, Integer o2) {
//            // TODO Auto-generated method stub
//            return o2.compareTo(o1);
//        }
//    });
//    Queue<GUDPPacket> ReceievedButUnconfirmedACK = new PriorityQueue<>(cmp);
////    PriorityQueue<Integer> NumberReceievedButUnconfirmedACK = new PriorityQueue<cmpint>;
//
//    public void receive(DatagramPacket packet) throws IOException {
//        byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
//        DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
//        datagramSocket.receive(udppacket);
//        GUDPPacket gudppacket = GUDPPacket.unpack(udppacket);
//        gudppacket.decapsulate(packet);
////        System.out.println("What I received:" + byteArrayToInt(gudppacket.getBytes()));
//        System.out.println("REC Yes!");
//
//        if (gudppacket.getType() != GUDPPacket.TYPE_ACK){
//            int seqNo = gudppacket.getSeqno();
//            String destinationIP = packet.getAddress().getHostAddress();
//            int destinationPort = 10000;
//            System.out.println("Sent ACK Packet!");
//            BuildandSendACKPacket(seqNo + 1, destinationIP, destinationPort);
//        }
////        else {
////            byte[] data = packet.getData();
////            int plength = packet.getLength();
////            ByteBuffer buffer = ByteBuffer.wrap(data, 0, plength);
////            System.out.println("What I received ACK num is" + byteArrayToInt(buffer.array()));
////            int need_send_seq = byteArrayToInt(buffer.array());
////        }
//    }
//
//
//    public class ProcessACK implements Runnable{
//        // To remove ACKed Packet
//        public ProcessACK(){
//        }
//
//        @Override
//        public void run(){
//            System.out.println("ProcessACK start!");
//            while (true){
//                if(ReceievedButUnconfirmedACK.isEmpty())
//                    continue;
//                while (!SentNotACK.isEmpty()){
////                    GUDPPacket topRBC = ReceievedButUnconfirmedACK.peek();// num in Byte
//                    if (NumberReceievedButUnconfirmedACK.isEmpty())
//                        continue;
//                    int received_num = NumberReceievedButUnconfirmedACK.peek();
//                    GUDPPacket topSNA = SentNotACK.peek();  // num in Seq
//                    byte[] dataACK = new byte[GUDPPacket.MAX_DATA_LEN];
//                    DatagramPacket dp = new DatagramPacket(dataACK, dataACK.length);
////                    topRBC.getPayload(dataACK,GUDPPacket.MAX_DATA_LEN);
////                    System.out.println("ProcessACK: toRBC data is" + String.valueOf(topRBC.getBytes()));
////                    int ACKdata = Integer.parseInt(String.valueOf(topRBC.getBytes()));
////                    System.out.println("ACK number is" + ACKdata);
//                    if (received_num - 1 == topSNA.getSeqno()){
////                        System.out.println("ACKed" + (ACKdata-1));
//                        // reset timer
//                        NumberReceievedButUnconfirmedACK.poll();
//                        SentNotACK.poll();
//                    }
//                }
//            }
//        }
//    }
//
//    public class ACKMonitor implements Runnable{
//        // To receive ACK packet
//        public ACKMonitor(){
//        }
//
//        @Override
//        public void run() {
//            System.out.println("ACKMonitor start!");
//            DatagramSocket ACKdatagramSocket;
//            try {
//                ACKdatagramSocket = new DatagramSocket(10000);
//            } catch (SocketException e) {
//                throw new RuntimeException(e);
//            }
//            GUDPSocket gudpSocket = new GUDPSocket(ACKdatagramSocket);
//            while (true){
//                byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
//                DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
//                try {
//                    gudpSocket.receive(udppacket);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                System.out.println("ACKmonitor working!!");
////                GUDPPacket gudppacket = null;
//////                try {
////                try {
////                    gudppacket = GUDPPacket.unpack(udppacket);
////                } catch (IOException e) {
////                    throw new RuntimeException(e);
////                }
////                } catch (IOException e) {
////                    throw new RuntimeException(e);
////                }
////                String text = new String(udppacket.getData(), 0, udppacket.getLength());
////                System.out.println("Ready to Show:\n" + text);
////                ReceievedButUnconfirmedACK.add(gudppacket);
//
//                byte[] data = udppacket.getData();
//                int plength = udppacket.getLength();
//                ByteBuffer buffer = ByteBuffer.wrap(data, 0, plength);
//                System.out.println("What I received ACK num is" + byteArrayToInt(buffer.array()));
//                int received_seq = byteArrayToInt(buffer.array());
//                if (startData == false)
//                    NumberReceievedButUnconfirmedACK.add(received_seq);
////                if (gudppacket.getType() == GUDPPacket.TYPE_BSN){
//                startData = true;
//                last_seq = received_seq;
////                }
//            }
//        }
//    }
//
//    public int last_seq;
//
//    public class Sender implements Runnable{
//
//        public Sender(){
//        }
//
//        @Override
//        public void run() {
//            System.out.println("Sender Start!");
//
////            System.out.println("in run()");
//            while (!gudpDataBuffer.isEmpty()){
//                if(!startData){
//                    System.out.println(startData);
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                    continue;
//                }
//                if(SentNotACK.size() < window_Size){
//                    GUDPPacket gudpDataPacket = gudpDataBuffer.poll();
//                    gudpDataPacket.setType(GUDPPacket.TYPE_DATA);
//                    last_seq += 1;
//                    gudpDataPacket.setSeqno(last_seq);
//                    SentNotACK.add(gudpDataPacket);
//                    try {
//                        send_Packet(gudpDataPacket);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
////                SentNotACK.poll(); // give up ACK
//            }
//        }
//    }
//
//    public void BuildandSendBSNPacket(String destinationIP, int destination_port) throws IOException {
////        Random r = new Random();
//        int startNum = 1;
//        last_seq = startNum;
//        byte[] buf = intToByteArray(startNum);
//        System.out.println("Sent BSN Number is" + byteArrayToInt(buf));
//        DatagramPacket ackPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName(destinationIP), destination_port);
//        byte[] data = ackPacket.getData();
//        int plength = ackPacket.getLength();
//        ByteBuffer buffer = ByteBuffer.wrap(data, 0, plength);
//        System.out.println("ACKpacket Sent BSN Number Check: " + byteArrayToInt(buffer.array()));
//        GUDPPacket gudppacket = GUDPPacket.encapsulate(ackPacket);
//        gudppacket.setType(GUDPPacket.TYPE_BSN);
////        byte[] buf_test = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
////        DatagramPacket udppacket = new DatagramPacket(buf_test, buf_test.length);
////        gudppacket.
////        gudppacket.decapsulate(ackPacket);
////        System.out.println("gudppacket Sent BSN Number Check: " + byteArrayToInt(gudppacket.getBytes()));
//        send_Packet(gudppacket);
//        SentNotACK.add(gudppacket);
//    }
//
//    public boolean startData = false;
//
//    public void finish() throws IOException {
//        // To receive ACK packet
//        ACKMonitor ackMonitor = new ACKMonitor();
//        Sender sender = new Sender();
//        ProcessACK processACK = new ProcessACK();
//        Thread senderThread = new Thread(sender);
//        Thread ackMonitorThread = new Thread(ackMonitor);
//        Thread processACKThread = new Thread(processACK);
//        System.out.println("Start?");
//        ackMonitorThread.start();
//        processACKThread.start();
//        senderThread.start();
//        System.out.println("Start!");
//        System.out.println("How many data in buffer?    " + gudpDataBuffer.size());
//        System.out.println("How many data in SentNotACK?    " + SentNotACK.size());
//
//        GUDPPacket tmp = gudpDataBuffer.peek();
////        String destinationIP = tmp.getSocketAddress().getAddress().getHostAddress();
////        System.out.println("Test!!!" + destinationIP);
////        int destinationPort = 10000;
////        BuildandSendBSNPacket(destinationIP, destinationPort);
////        while (!SentNotACK.isEmpty()){
////
////        }
//        while (true){
//            if (!startData)
//                continue;
//            System.out.println("It's time to sent " + last_seq);
//            System.out.println("SentNotACK Size is " + SentNotACK.size());
//            System.out.println("ReceievedButUnconfirmedACK Size is " + NumberReceievedButUnconfirmedACK.size());
//            System.out.println("gudpBuffer Size is " + gudpDataBuffer.size());
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            if(SentNotACK.isEmpty() && ReceievedButUnconfirmedACK.isEmpty() && gudpDataBuffer.isEmpty()){
//                close();
//                break;
//            }
//        }
//    }
//    public void close() throws IOException {
//        System.out.println("Close and End!");
//        startData = false;
//    }
//}
//
