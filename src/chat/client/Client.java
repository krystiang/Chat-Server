package chat.client;

import static chat.Protocol.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private String name;
    private Connection connection;
    private DatagramSocket outSocket;
    private MessageListener listener;
    private boolean isConnected;

    public Client(InetAddress serverAddr, MessageReceiver receiver) throws IOException {
        this.connection = new Connection(serverAddr);
        this.outSocket = new DatagramSocket();
        this.listener = new MessageListener(receiver);
        this.isConnected = true;
        
        new Thread(listener).start();
    }

    public String getName() {
        return name;
    }

    public boolean setName(String name) {
        if (isValidName(name)) {
            this.name = name;
            String resp = connection.request("NEW " + name);
            return resp.equals("OK");
        } else {
            return false;
        }
    }
    
    public void disconnect() {
        connection.request("BYE");
        listener.stop();
        connection.close();
        this.isConnected = false;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    boolean isValidName(String name) {
        return name.matches(NAME_PATTERN);
    }
    
    /**
     * Liste aller im Chat enthaltenen Benutzer
     * 
     * @return leer bei Misserfolg, gefuellt bei Erfolg
     */
    Map<String, InetAddress> getClients() {
        Map<String, InetAddress> users = new HashMap<String, InetAddress>();
        
        String resp = connection.request("INFO");
        boolean error = resp.startsWith("ERROR");
        
        if (!error) {
            try {
                Scanner scanner = new Scanner(resp);
                scanner.skip("LIST");
                
                int numPairs = scanner.nextInt();
                for (int i = 0; i < numPairs; ++i) {
                    InetAddress ip = InetAddress.getByName(scanner.next());
                    String name = scanner.next();
                    users.put(name, ip);
                }
                scanner.close();
            } catch (Exception e) {
                error = true;
                e.printStackTrace();
            }
        }
        
        if (error) {
            users.clear();
        }
        
        return users;
    }
    
    /**
     * Send message to all the other chat clients.
     * Wenn msg.length() > 100, wird der hintere Teil der zu uebersendenden Nachricht abgeschnitten
     * 
     * @param msg
     */
    public void sendMessage(String msg) {
        try {
                for (Map.Entry<String, InetAddress> e : getClients().entrySet()) {
                		String data = formatData(getName(),msg);
                    outSocket.send(new DatagramPacket(data.getBytes(), 0, data.length(), e.getValue(), CLIENT_PORT));
                }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    /**
     * @param name
     * @param msg
     * @return
     * Formatiert die zu uebertragenen Daten in ein gewolltes Format NAME:<SPACE>NACHRICHT<\n>
     */
    public static String formatData(String name, String msg) {
        return String.format("%s: %s\n", name, msg.substring(0, msg.length()));
    }
    
    public static void main(String[] args) {
        System.out.println("Start client");
        
        final Controller controller = new Controller();
        final Chat chat = new Chat(controller);
        controller.setView(chat);
        controller.run();
    }
}
