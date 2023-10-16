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
//            // 使用 DatagramPacket 将数据封装到该对象的包中
//            byte[] buf = line.getBytes();
//            DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName("127.0.0.1"), 10000);
//            System.out.println(dp.getAddress().getHostAddress());// This is destination address.
//            System.out.println(dp.getPort()); //This is the destination port
////            System.out.println(dp.);
//            // 通过 UDP 的 Socket 服务将数据包发送出去，使用 send 方法
//            gudpSocket.send(dp);
//            // 如果输入信息为 over，则结束循环
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
