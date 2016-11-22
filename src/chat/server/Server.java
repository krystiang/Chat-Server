package chat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import chat.Protocol;

public class Server {
	private final ServerSocket socket;
	private final ThreadPoolExecutor threads = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
	
	private Map<InetAddress, String> clients = new HashMap<InetAddress, String>();
	
	public Server(Integer port) throws IOException {
		socket = new ServerSocket(port);
	}
	
	public void run() {
		try {
			System.out.println("Waiting for incomming connection");
			while (true) {
				if (threads.getCorePoolSize() > threads.getActiveCount()) {
					threads.execute(new Connection(socket.accept(), this));
				}
			}
		} catch (IOException e) {
			threads.shutdown();
		}		
	}

	public boolean addClient(String name, InetAddress connection) {
		if (!isClientTaken(name)) {
		    if (clients.containsKey(connection)) {
                System.out.println(String.format("%s is now known as %s", clients.get(connection), name));
		    } else {
    			System.out.println(String.format("New Client %s (%s)", name, connection));
		    }

            clients.put(connection, name);
			return true;
		}
		else {
			return false;
		}
	}
	
	public void removeClient(InetAddress connection) {
        System.out.println(String.format("%s (%s) left", clients.get(connection), connection));
	    clients.remove(connection);
	}

	public boolean isClientTaken(String name) {
		return clients.containsValue(name);
	}

	public Map<InetAddress, String> getClients() {
		return new HashMap<InetAddress, String>(this.clients);
	}
	
	public boolean isIdentified(InetAddress con) {
	    return clients.containsKey(con);
	}
	
	public static void main(String[] args) {
		try {
			System.out.println("Starting server");
			new Server(Protocol.SERVER_PORT).run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
