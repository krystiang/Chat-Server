package chat.client;

import static chat.Protocol.CLIENT_PORT;
import static chat.Protocol.MAX_MSG_LEN;
import static chat.Protocol.MAX_NAME_LEN;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Der MessageListener horcht auf den port Protocol.CLIENT_PORT nach
 * ankommenden Nachrichten und gibt sie an den MessageReceiver weiter,
 * damit diese von der Chat GUI uebernommen werden
 *
 */
public class MessageListener implements Runnable {
    private MessageReceiver receiver;
    private DatagramSocket socket;
    
    public MessageListener(MessageReceiver receiver) {
        this.receiver = receiver;
    }
    
    public void stop() {
        socket.close();
    }
    
    @Override
    public void run() {
        try {
            socket = new DatagramSocket(CLIENT_PORT);
            // calculate buffer size
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 0; i < MAX_NAME_LEN; ++i) { nameBuilder.append('\0'); }
            
            StringBuilder msgBuilder = new StringBuilder();
            for (int i = 0; i < MAX_MSG_LEN; ++i) { msgBuilder.append('\0'); }
            
            String longestPossibleData = Client.formatData(nameBuilder.toString(), msgBuilder.toString());
            
            byte[] buf = longestPossibleData.getBytes();
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            
            while (true) {
                // clear the buffer
                for (int i = 0; i < buf.length; ++i) { buf[i] = '\0'; }
                
                socket.receive(p);
                int paketlength = p.getLength();
                receiver.receiveMessage(new String(p.getData()).substring(0, paketlength));
                System.out.println("received");
            }
        } catch (SocketException e) {
            // socket closed
            // everything's okay
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
