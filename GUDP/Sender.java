//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//
//public class Sender {
//
//
////    public void
//
//    public static void main(String[] args) throws IOException{
//
//
//        System.out.println("Sender start......");
//
//        String line = null;
//        BufferedReader bufr = new BufferedReader(new InputStreamReader(System.in));
//        DatagramSocket ds = new DatagramSocket();
//        GUDPSocket_old gudpSocket = new GUDPSocket_old(ds);
//        while ((line = bufr.readLine()) != null) {
//            // ʹ�� DatagramPacket �����ݷ�װ���ö���İ���
//            byte[] buf = line.getBytes();
//            DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName("127.0.0.1"), 10000);
//            System.out.println(dp.getAddress().getHostAddress());// This is destination address.
//            System.out.println(dp.getPort()); //This is the destination port
////            System.out.println(dp.);
//            // ͨ�� UDP �� Socket �������ݰ����ͳ�ȥ��ʹ�� send ����
//            gudpSocket.send(dp);
//            // ���������ϢΪ over�������ѭ��
//            if ("over".equals(line)){
//                gudpSocket.finish();
//                System.out.println("Over in");
//                break;
//            }
//        }
//
//        System.out.println("Over out");
//    }
//
//}
