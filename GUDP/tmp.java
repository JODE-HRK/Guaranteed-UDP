//import java.io.IOException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetSocketAddress;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.*;
//import java.util.PriorityQueue;
//import java.util.Queue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//
//public class GUDPSocket implements GUDPSocketAPI {
//    private final DatagramSocket datagramSocket;
//    public GUDPReceiver receive = new GUDPReceiver();
//    ;
//    public Thread receiver = new Thread(receive, "GUDP Receiver");
//    ;
//    public GUDPSender send = new GUDPSender(GUDPPacket.MAX_WINDOW_SIZE);
//    public Thread sender = new Thread(send, "GUDP Sender");
//    public boolean receivestate = false;
//    public GUDPSocket(DatagramSocket socket) throws SocketException {
//        datagramSocket = socket;
//    }
//    public void send(DatagramPacket packet) throws IOException {
//        send.addDataToSendingQueue(packet);
//    }
//    public void receive(DatagramPacket packet) throws IOException {
//        if (!receivestate) {
//            receiver.start();
//            receivestate = true;
//            System.out.println("receiver start");
//        }
//        receive.receive(packet);
//    }
//    public void finish() throws IOException {
//        sender.start();
//        System.out.println("sender start");
//    }
//    public void close() throws IOException {
//
//    }
//
//    class GUDPBuffer {
//        final Duration TIMEOUT = Duration.ofMillis(1000);
//        final GUDPPacket packet;
//        Instant resendTime;
//        boolean sent = false;
//        boolean ackReceived;
//        int retrytimes = 0;
//        public GUDPBuffer(GUDPPacket packet) {
//            this.packet = packet;
//        }
//
//        public void markSent() {
//            resendTime = Instant.now()
//                    .plus(TIMEOUT)
//                    .minusNanos(1);
//            sent = true;
//        }
//
//        public boolean isTimeout() {
//            return sent && !ackReceived && Instant.now().isAfter(resendTime);
//        }
//    }
//
//    class GUDPSender implements Runnable {
//        private final int dataWindowSize;
//        public int currentWindowSize;
//        public int firstIndex;
//        public int lastIndex;
//        public final List<GUDPBuffer> sendingqueue = new ArrayList<>();
//        public final List<Integer> ACKBuffer = new ArrayList<>();
//        public InetSocketAddress address;
//        public int bsnSeqNo;
//        public int seqNo;
//        public boolean sendingexit = false;
//        public int count = 0;
//        public int ackbufferindex=0;
//        public double sendpercent=0;
//
//        public GUDPSender(int dataWindowSize) throws SocketException {
//            this.dataWindowSize = dataWindowSize;
//            this.addBsnToSendingQueue();
//        }
//
//        public void addBsnToSendingQueue() {
//            bsnSeqNo = new Random().nextInt(Short.MAX_VALUE);
//            seqNo = bsnSeqNo;
//            currentWindowSize = 1;
//            firstIndex = 0;
//            lastIndex = 1;
//            ByteBuffer buffer = ByteBuffer.allocate(GUDPPacket.HEADER_SIZE);
//            buffer.order(ByteOrder.BIG_ENDIAN);
//            GUDPPacket packet = new GUDPPacket(buffer);
//            packet.setType(GUDPPacket.TYPE_BSN);
//            packet.setVersion(GUDPPacket.GUDP_VERSION);
//            packet.setSeqno(seqNo);
//            packet.setPayloadLength(0);
//            seqNo++;
//            sendingqueue.add(new GUDPBuffer(packet));
//        }
//
//        public void addDataToSendingQueue(DatagramPacket packet) throws IOException {
//            address = (InetSocketAddress) packet.getSocketAddress();
//            GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
//            gudppacket.setSeqno(seqNo);
//            seqNo++;
//            sendingqueue.add(new GUDPBuffer(gudppacket));
//        }
//
//        public void checkAck() throws IOException {
//            for (int index = firstIndex; index < lastIndex; index++) {
//                GUDPBuffer packetinqueue = sendingqueue.get(index);
//                if (packetinqueue.isTimeout()&packetinqueue.retrytimes<20) {
//                    send(packetinqueue);
//                    packetinqueue.retrytimes++;
//                    System.out.println("resend packet"+(packetinqueue.packet.getSeqno()-bsnSeqNo)+":"+packetinqueue.retrytimes+"times");
//                }
//                if(packetinqueue.retrytimes==20){
//                    this.stop();
//                    System.out.println("too many retry times,sender close");
//                }
//            }
//        }
//
//        public void updatewindow(List<Integer> ACKBuffer) throws IOException {
//            for (int i=ackbufferindex; i < ACKBuffer.size(); i++) {
//                int index = ACKBuffer.get(i) - 1 - bsnSeqNo;
//                GUDPBuffer packetinqueue = sendingqueue.get(index);
//                if(!packetinqueue.ackReceived) {
//                    packetinqueue.ackReceived = true;
//                    count++;
//                    ackbufferindex++;
//                    currentWindowSize = dataWindowSize;
//                }
//            }
//            updatefirstIndex();
//            if(count!=0&count!=sendingqueue.size()){
//                sendpercent = (double)count/sendingqueue.size()*100;
//
//                if(sendpercent>0){
//                    System.out.println("already sent:"+String.format("%.2f",sendpercent)+"%");
//                }
//                sendDataPackets();
//            }
//            if(count==sendingqueue.size()){
//                System.out.println("send completed");
//                stop();
//            }
//
//        }
//
//        public void updatefirstIndex() {
//            for (int index = firstIndex; index < lastIndex; index++) {
//                GUDPBuffer block = sendingqueue.get(index);
//                if (block.ackReceived) {
//                    firstIndex++;
//                    if(firstIndex==1){
//                        System.out.println("send data");
//                    }
//                    continue;
//                }
//                break;
//            }
//        }
//
//        public void sendBSN() throws IOException {
//            send(sendingqueue.get(0));
//        }
//        public void send(GUDPBuffer packet) throws IOException {
//            packet.markSent();
//            packet.packet.setSocketAddress(address);
//            datagramSocket.send(packet.packet.pack());
//        }
//        private void sendDataPackets() throws IOException {
//            int newlastIndex = Math.min(sendingqueue.size(), firstIndex + currentWindowSize);
//            for (int index = firstIndex; index < newlastIndex; index++) {
//                GUDPBuffer block = sendingqueue.get(index);
//                if (!block.sent) {
//                    send(block);
//                }
//            }
//            lastIndex = newlastIndex;
//        }
//        public void stop() {
//            sendingexit = true;
//            System.out.println("sender close");
//        }
//
//        public void run() {
//            ACKReceiver ack = null;
//            try {
//                ack = new ACKReceiver(ACKBuffer);
//            } catch (SocketException e) {
//                throw new RuntimeException(e);
//            }
//            Thread ackreceiver = new Thread(ack, "ACK Receiver");
//            ackreceiver.start();
//            System.out.println("ackreceiver start");
//            try {
//                sendBSN();
//                System.out.println("sendBSN");
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            while (!sendingexit) {
//                try {
//                    updatewindow(ACKBuffer);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                try {
//                    checkAck();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            ack.stop();
//
//        }
//    }
//
//    class GUDPReceiver implements Runnable {
//
//        private final Queue<GUDPPacket> Packetsbuffer = new PriorityQueue<>(Comparator.comparingInt(GUDPPacket::getSeqno));
//        private final BlockingQueue<GUDPPacket> receivingQueue = new LinkedBlockingQueue<>();
//        private int expectSeqNo=0;
//        private boolean running = true;
//        public GUDPReceiver() throws SocketException {
//        }
//        public void receive(DatagramPacket packet) throws IOException {
//            try {
//                GUDPPacket gudpPacket = receivingQueue.take();
//                gudpPacket.decapsulate(packet);
//                VSFtp vspacket = new VSFtp(packet);
//                if (vspacket.getType() == VSFtp.TYPE_END){
//                    System.out.println("receive completed");
//                    this.stop();
//                }
//            } catch (InterruptedException e) {
//                throw new IOException(e);
//            }
//        }
//        private void receivepackets() throws IOException {
//            byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
//            DatagramPacket udpPacket = new DatagramPacket(buf, buf.length);
//            datagramSocket.receive(udpPacket);
//            GUDPPacket gudppacket = GUDPPacket.unpack(udpPacket);
//            short type = gudppacket.getType();
//            switch (type) {
//                case GUDPPacket.TYPE_BSN:
//                    expectSeqNo = gudppacket.getSeqno() + 1;
//                    Packetsbuffer.add(gudppacket);
//                    sendAck(gudppacket);
//                    break;
//                case GUDPPacket.TYPE_DATA:
//                    Packetsbuffer.add(gudppacket);
//                    sendAck(gudppacket);
//                    break;
//            }
//        }
//        private void sendAck(GUDPPacket Packet) throws IOException {
//            ByteBuffer buffer = ByteBuffer.allocate(GUDPPacket.HEADER_SIZE);
//            buffer.order(ByteOrder.BIG_ENDIAN);
//            GUDPPacket ackPacket = new GUDPPacket(buffer);
//            ackPacket.setType(GUDPPacket.TYPE_ACK);
//            ackPacket.setVersion(GUDPPacket.GUDP_VERSION);
//            ackPacket.setSeqno(Packet.getSeqno() + 1);
//            ackPacket.setPayloadLength(0);
//            InetSocketAddress ADDR = new InetSocketAddress(Packet.getSocketAddress().getAddress(),(datagramSocket.getLocalPort()-1));
//            ackPacket.setSocketAddress(ADDR);
//            datagramSocket.send(ackPacket.pack());
//        }
//        private void saveToReceivingQueue() {
//            while (true) {
//                GUDPPacket packet = Packetsbuffer.peek();
//                if (packet == null) {
//                    break;
//                }
//                int seqNo = packet.getSeqno();
//                if (seqNo < expectSeqNo) {
//                    Packetsbuffer.remove();
//                    continue;
//                }
//                Packetsbuffer.remove();
//                receivingQueue.add(packet);
//                expectSeqNo++;
//            }
//        }
//        public void run() {
//            while (this.running) {
//                try {
//                    receivepackets();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                saveToReceivingQueue();
//            }
//        }
//        public void stop() {
//            System.out.println("receiver close");
//            this.running = false;
//            System.exit(1);
//        }
//    }
//    class ACKReceiver implements Runnable {
//        public final List<Integer> Buffer;
//        public DatagramSocket ackreceivesocket;
//        public boolean ackreceiverrunning = true;
//        public ACKReceiver(List<Integer> ACKBuffer) throws SocketException {
//            this.Buffer = ACKBuffer;
//            this.ackreceivesocket = new DatagramSocket((send.address.getPort()-1));
//        }
//        private void receiveACK() throws IOException {
//            byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
//            DatagramPacket udpPacket = new DatagramPacket(buf, buf.length);
//            ackreceivesocket.receive(udpPacket);
//            GUDPPacket gudppacket = GUDPPacket.unpack(udpPacket);
//            short type = gudppacket.getType();
//            if (type == GUDPPacket.TYPE_ACK) {
//                Buffer.add(gudppacket.getSeqno());
//            }
//        }
//        public void stop(){
//            ackreceiverrunning = false;
//            System.out.println("ACKReceiver Close");
//            System.exit(1);
//        }
//        public void run() {
//            while (ackreceiverrunning) {
//                try {
//                    receiveACK();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }
//}
//
