//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.nio.ByteBuffer;
//import java.util.LinkedList;
//import java.util.PriorityQueue;
//import java.util.Queue;
//
//public class GUDPSocket implements GUDPSocketAPI {
//    DatagramSocket datagramSocket;
//
//    public boolean start = false;
//
//    public GUDPSocket(DatagramSocket socket) {
//        datagramSocket = socket;
//    }
//
//    public Queue<GUDPPacket> gudpDataBuffer = new LinkedList<>();
//
//    public void send_Packet(GUDPPacket gudpPacket) throws IOException {
//        DatagramPacket udppacket = gudpPacket.pack();
//        datagramSocket.send(udppacket);
//    }
//
//    public int seq_no = 1;
//    public void send(DatagramPacket packet) throws IOException {
//        GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
//        gudppacket.setSeqno(seq_no);
//        seq_no += 1;
//        gudpDataBuffer.add(gudppacket);
//
////        GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
////        DatagramPacket udppacket = gudppacket.pack();
////        datagramSocket.send(udppacket);
//    }
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
//    public void receive(DatagramPacket packet) throws IOException {
//        byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
//        DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
//        datagramSocket.receive(udppacket);
//        GUDPPacket gudppacket = GUDPPacket.unpack(udppacket);
//        gudppacket.decapsulate(packet);
//
//        if (gudppacket.getType() != GUDPPacket.TYPE_ACK){
////            if (gudppacket.getType() == GUDPPacket.TYPE_BSN)
////                start = true;
//            int seqNo = gudppacket.getSeqno();
//            String destinationIP = packet.getAddress().getHostAddress();
//            int destinationPort = 10000;
//            BuildandSendACKPacket(seqNo + 1, destinationIP, destinationPort);
//            System.out.println("Sent ACK Packet! " + (seqNo + 1));
//        }
////        else {  // in Thread
////            byte[] data = packet.getData();
////            int plength = packet.getLength();
////            ByteBuffer buffer = ByteBuffer.wrap(data, 0, plength);
////            System.out.println("What I received ACK num is" + byteArrayToInt(buffer.array()));
////            sen_seq = byteArrayToInt(buffer.array());
////        }
//    }
//    public Queue<GUDPPacket> SentNotACK = new LinkedList<>();
//    public int WindowSize = 1;
//
//    PriorityQueue<Integer> ACKedNotRemove = new PriorityQueue<Integer>();
//
//    public class ACKMonitor implements Runnable{
//        // To receive ACK packet and remove ACKed packet
//        DatagramSocket ACKdatagramSocket;
//        public ACKMonitor() throws SocketException {
//            ACKdatagramSocket = new DatagramSocket(10000);
//        }
//
//        @Override
//        public void run() {
////            System.out.println("ACKMonitor start!");
//            GUDPSocket gudpSocket = new GUDPSocket(ACKdatagramSocket);
//            while (true){
//                if (SentNotACK.isEmpty() && gudpDataBuffer.isEmpty() && ACKedNotRemove.isEmpty()){
//                    System.out.println("ACKMonitor exit!");
//                    break;
//                }
////                System.out.println("ACKmonitor working!!");
//
//                byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
//                DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
////                System.out.println("test!!!start");
//                try {
//                    gudpSocket.receive(udppacket);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
////                System.out.println("test!!!end");
////                System.out.println("Is SentNot ACK empty? 1" + SentNotACK.isEmpty());
//
//                byte[] data = udppacket.getData();
//                int plength = udppacket.getLength();
//                ByteBuffer buffer = ByteBuffer.wrap(data, 0, plength);
//                System.out.println("What I received ACK num is" + byteArrayToInt(buffer.array()));
//                int received_seq = byteArrayToInt(buffer.array());
//                ACKedNotRemove.add(received_seq);
////                System.out.println("Is SentNot ACK empty? 2" + SentNotACK.isEmpty());
////                System.out.println("ACKedNotRemove is empty? " + ACKedNotRemove.peek());
////                System.out.println("SentNotACK is empty? " + SentNotACK.peek().getSeqno());
//                if (ACKedNotRemove.isEmpty() || SentNotACK.isEmpty())
//                    continue;
//                while (ACKedNotRemove.peek() == SentNotACK.peek().getSeqno() + 1){
//                    ACKedNotRemove.poll();
//                    SentNotACK.poll();
//                    if (ACKedNotRemove.isEmpty() || SentNotACK.isEmpty())
//                        break;
//                }
//                System.out.println("ACKedNotRemove is empty? " + ACKedNotRemove.isEmpty());
//                System.out.println("SentNotACK is empty? " + SentNotACK.isEmpty());
//            }
//        }
//    }
//
//    public void finish() throws IOException {
//        ACKMonitor ackMonitor = new ACKMonitor();
//        Thread ackMonitorThread = new Thread(ackMonitor);
//        ackMonitorThread.start();
//        System.out.println(gudpDataBuffer.size());
//        System.out.println(start);
//        while (!gudpDataBuffer.isEmpty()){
//            if (!start){
//                GUDPPacket bsnPacket = gudpDataBuffer.poll();
//                bsnPacket.setType(GUDPPacket.TYPE_BSN);
//                SentNotACK.add(bsnPacket);
//                send_Packet(bsnPacket);
//                System.out.println("What I send num is " + bsnPacket.getSeqno());
//                while (true){
//                    if (SentNotACK.isEmpty())
//                        break;
//                    System.out.println("IN " + SentNotACK.size());
//                }
//                start = true;
//                System.out.println("Start???? " + start);
//            }
//            else {
//                System.out.println("SentNotACK " + SentNotACK.size());
//                System.out.println("ACKedNotRemove " + ACKedNotRemove.size());
//                System.out.println("gudpDataBuffer " + gudpDataBuffer.size());
//                if (SentNotACK.size() >= WindowSize)
//                    continue;
//                GUDPPacket dataPacket = gudpDataBuffer.poll();
//                dataPacket.setType(GUDPPacket.TYPE_DATA);
//                System.out.println("What I send num is " + dataPacket.getSeqno());
//                SentNotACK.add(dataPacket);
//                send_Packet(dataPacket);
//            }
//            System.out.println(gudpDataBuffer.size());
//        }
//        while (true){
//            System.out.println("Exiting");
//            if (SentNotACK.isEmpty() && ACKedNotRemove.isEmpty() && gudpDataBuffer.isEmpty()){
//                System.out.println("interrupt");
//                ackMonitorThread.interrupt();
//                break;
//            }
//            System.out.println("SentNotACK is " + SentNotACK.size() + " " + SentNotACK.peek().getSeqno());
//            System.out.println("ACKedNotRemove is " + ACKedNotRemove.size());
//            System.out.println("gudpDataBuffer is " + gudpDataBuffer.size());
//        }
//        start = false;
//    }
//    public void close() throws IOException {
////        start = false;
//        System.out.println("Close and End!");
//    }
//}
//
