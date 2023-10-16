import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GUDPSocket implements GUDPSocketAPI {
    DatagramSocket datagramSocket;
    public List<Integer> ackbuffer = new ArrayList<>();
    public GUDPReceiver receive = new GUDPReceiver(ackbuffer);
    public Thread receiver = new Thread(receive, "GUDP Receiver");
    public GUDPSender send = new GUDPSender(GUDPPacket.MAX_WINDOW_SIZE);
    public Thread sender = new Thread(send, "GUDP Sender");
    public boolean receiveState = false;

    public GUDPSocket(DatagramSocket socket) {
        datagramSocket = socket;
    }

    public void send(DatagramPacket packet) throws IOException {
//        GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
//        DatagramPacket udppacket = gudppacket.pack();
//        datagramSocket.send(udppacket);
        byte[] tmpData = packet.getData();
        ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int vsType = byteBuffer.getInt();
        if (vsType == 1)
            send.addBSN();
        send.addDataToQueue(packet);
    }

    public void receive(DatagramPacket packet) throws IOException {
//        byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
//        DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
//        datagramSocket.receive(udppacket);
//        GUDPPacket gudppacket = GUDPPacket.unpack(udppacket);
//        gudppacket.decapsulate(packet);
        if (!receiveState) {
            receiver.start();
            receiveState = true;
            System.out.println("Receiver Start");
        }
        receive.receive(packet);
    }

    public void finish() throws IOException {
//        if (!sender.isAlive())
        System.out.println("file loaded!");
    }
    public void close() throws IOException {
        sender.start();
        System.out.println("Sender Start");
    }

    private class GUDPBuffer {
        final Duration TIMEOUT = Duration.ofMillis(1000);
        final GUDPPacket packet;
        Instant resendTime;
        boolean sent = false;
        boolean ACKed = false;
        int retrytimes = 0;


        public GUDPBuffer(GUDPPacket packet) {
            this.packet = packet;
        }

        public void markSentTimestamp() {
            resendTime = Instant.now().plus(TIMEOUT).minusNanos(1);
            sent = true;
        }

        public boolean isTimeout() {
            return sent && !ACKed && Instant.now().isAfter(resendTime);
        }
    }

    private class GUDPReceiver implements Runnable{

        private final Queue<GUDPPacket> packetsBuffer = new PriorityQueue<>(Comparator.comparingInt(GUDPPacket::getSeqno));
        private final BlockingQueue<GUDPPacket> receivingQueue = new LinkedBlockingQueue<>();
        private int expectSeqNo = 0;
        public List<Integer> ackBuffer;
        private boolean running = true;

        public GUDPReceiver(List<Integer> ackBuffer) {
            this.ackBuffer = ackBuffer;
        }

        public void receive(DatagramPacket packet) throws IOException {
            try {
                GUDPPacket gudpPacket = receivingQueue.take();
                gudpPacket.decapsulate(packet);
            } catch (InterruptedException e) {
                throw new IOException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void receivepackets() throws IOException {
            byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
            DatagramPacket udpPacket = new DatagramPacket(buf, buf.length);
            datagramSocket.receive(udpPacket);
            GUDPPacket gudppacket = GUDPPacket.unpack(udpPacket);
            short type = gudppacket.getType();
            switch (type) {
                case GUDPPacket.TYPE_BSN:
                    expectSeqNo = gudppacket.getSeqno() + 1;
                    packetsBuffer.add(gudppacket);
                    sendAck(gudppacket);
                    break;
                case GUDPPacket.TYPE_DATA:
                    if(gudppacket.getSeqno()==expectSeqNo) {
                        packetsBuffer.add(gudppacket);
                        sendAck(gudppacket);
                        break;
                    }
                case GUDPPacket.TYPE_ACK:
                    ackBuffer.add(gudppacket.getSeqno());
                    break;
            }
        }

        private void sendAck(GUDPPacket Packet) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(GUDPPacket.HEADER_SIZE);
            buffer.order(ByteOrder.BIG_ENDIAN);
            GUDPPacket ackPacket = new GUDPPacket(buffer);
            ackPacket.setType(GUDPPacket.TYPE_ACK);
            ackPacket.setVersion(GUDPPacket.GUDP_VERSION);
            ackPacket.setSeqno(Packet.getSeqno() + 1);
            ackPacket.setPayloadLength(0);
            ackPacket.setSocketAddress(Packet.getSocketAddress());
            datagramSocket.send(ackPacket.pack());
        }

        private void saveToReceivingQueue(){
            while (true){
                GUDPPacket packet = packetsBuffer.peek();
                if (packet == null)
                    break;
                int seqNo = packet.getSeqno();
                if(seqNo < expectSeqNo)
                    packetsBuffer.remove();
                else{
                    packetsBuffer.remove();
                    receivingQueue.add(packet);
                    expectSeqNo++;
                }
            }
        }

        @Override
        public void run() {
            while (this.running){
                try {
                    receivepackets();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                saveToReceivingQueue();
            }
        }
    }

    private class GUDPSender implements Runnable{
        private final int WindowSize;
        private int WindowSizeNow;

        public int head;
        public int tail;
        public final List<GUDPBuffer> sendingQueue = new ArrayList<>();
        public final List<Integer> ACKBuffer = new ArrayList<>();
        public InetSocketAddress address;
        public int bsnSeqNo;
        public Queue<Integer> bsnSeqnpQueue = new LinkedList<Integer>();;
        public int seqNo;
        public boolean finish = false;
        public int count = 0;
        public int ackBufferIndex = 0;
        public double sendpercent = 0;

        private GUDPSender(int dataWindowSize) {
            this.WindowSize = dataWindowSize;
            this.WindowSizeNow = dataWindowSize;
//            this.addBSN();
        }

        public void addBSN(){
            bsnSeqNo = new Random().nextInt(Short.MAX_VALUE);
            seqNo = bsnSeqNo;
            head = 0;
            tail = 1;
            ByteBuffer buffer = ByteBuffer.allocate(GUDPPacket.HEADER_SIZE);
            buffer.order(ByteOrder.BIG_ENDIAN);
            GUDPPacket packet = new GUDPPacket(buffer);
            packet.setType(GUDPPacket.TYPE_BSN);
            packet.setVersion(GUDPPacket.GUDP_VERSION);
            packet.setSeqno(seqNo);
            packet.setPayloadLength(0);
            seqNo++;
            sendingQueue.add(new GUDPBuffer(packet));
            bsnSeqnpQueue.add(bsnSeqNo);
        }

        public void addDataToQueue(DatagramPacket packet) throws IOException {
            address = (InetSocketAddress) packet.getSocketAddress();
            GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
            gudppacket.setSeqno(seqNo);
            seqNo++;
            sendingQueue.add(new GUDPBuffer(gudppacket));
        }

        public void updateWindow(List<Integer> ACKBuffer) throws IOException {
            for (int i = ackBufferIndex; i < ACKBuffer.size(); i++){
                int index = ACKBuffer.get(i) - 1 - bsnSeqNo;
                GUDPBuffer packetQueue = sendingQueue.get(index);
                if (!packetQueue.ACKed){
                    packetQueue.ACKed = true;
                    count++;
                }
                ackBufferIndex++;
            }
            updateHead();

            if (count != sendingQueue.size()) {
//                sendpercent = (double) count / sendingQueue.size() * 100;

                System.out.println("already sent:" + count + "/" + sendingQueue.size());
                sendDataPackets();
            }
            if (count == sendingQueue.size()) {
                System.out.println("sender completed");
                stop();
            }

            if (count == sendingQueue.size()){
                System.out.println("Sender Over");
                stop();
            }
        }

        public void updateHead(){
            for (int i = head; i < tail; i++){
                GUDPBuffer blocked = sendingQueue.get(i);
                if(blocked.ACKed){
                    if (blocked.packet.getType() == GUDPPacket.TYPE_BSN){
                        bsnSeqNo = bsnSeqnpQueue.poll();
                    }
                    head++;
                    if (head == 1){
                        System.out.println("Start Sender");
                    }
                    continue;
                }
                break;
            }
        }

        public void resendTimeout() throws IOException {
            for (int i = head; i < tail; i++){
                GUDPBuffer packetInQueue = sendingQueue.get(i);
                if (packetInQueue.isTimeout() && packetInQueue.retrytimes < 20){
                    send(packetInQueue);
                    packetInQueue.retrytimes++;
                    System.out.println("Resend " + (packetInQueue.packet.getSeqno()));
                }
//                else{
                if (packetInQueue.retrytimes == 20){
                    System.out.println(packetInQueue.packet.getSeqno() + " packet tried too many times!");
                    this.stop();
                }
//                }

            }
        }

        private void stop() {
            finish = true;
            System.out.println("Sender Close");
            System.exit(1);
        }

        public void send(GUDPBuffer packet) throws IOException {
            packet.markSentTimestamp();
            packet.packet.setSocketAddress(address);
            datagramSocket.send(packet.packet.pack());
        }

        private void sendDataPackets() throws IOException {
            int newTail = Math.min(sendingQueue.size(), head + WindowSizeNow);
            for (int i = head; i < newTail; i++){
                GUDPBuffer packet = sendingQueue.get(i);
                if (!packet.sent) {
                    send(packet);
                }
            }
            tail = newTail;
        }

        @Override
        public void run() {
            bsnSeqNo = bsnSeqnpQueue.poll();
            GUDPReceiver ackReceiver = new GUDPReceiver(ACKBuffer);
            Thread AckReceiver = new Thread(ackReceiver, "ACK Receiver");
            AckReceiver.start();
            System.out.println("ACKReceiver start");
            while (!finish){
                try {
                    updateWindow(ACKBuffer);
                    resendTimeout();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

