import java.io.IOException; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.net.SocketException;

public class Server{

    public static void main(String[] args) throws IOException{

        DatagramSocket serverSocket = new DatagramSocket(1234);

        byte[] receive = new byte[65535]; 

        DatagramPacket receivePacket = null;
        int messageN = 0;

        while (true) { 

            receivePacket = new DatagramPacket(receive, receive.length); 
            serverSocket.receive(receivePacket);

            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

            if(message.equals("Exit")){
                break;
            }
            else{
                System.out.println("Received from client: " + message);

                String response = "ACK " + String.valueOf(messageN);
                messageN += 1;
                byte[] sendData = response.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(
                        sendData,
                        sendData.length,
                        receivePacket.getAddress(),
                        receivePacket.getPort()
                );

                serverSocket.send(sendPacket);
            }

            receive = new byte[65535];
        }
 
        serverSocket.close();
    }
}