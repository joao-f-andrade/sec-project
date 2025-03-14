import java.io.IOException; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.util.Scanner;

public class Client{

    public static void main(String args[]) throws IOException{

        Scanner sc = new Scanner(System.in);

        DatagramSocket ds = new DatagramSocket();

        byte buf[] = null;

        InetAddress serverAddress = InetAddress.getByName("localhost");

        while (true) { 
            String inp = sc.nextLine(); 

            buf = inp.getBytes(); 

            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, serverAddress, 1234); 

            ds.send(DpSend);

            if (inp.equals("Exit")) 
                break;

            byte[] receiveData = new byte[24];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            ds.receive(receivePacket);
            
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received from server: " + response); 
        }

        ds.close();
    } 
}