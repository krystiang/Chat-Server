package chat.server;

import static chat.Protocol.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Connection implements Runnable {
	private Socket socket = null;
	private Server server;

	public Connection(Socket socket, Server server) {
		this.setSocket(socket);
		this.server = server;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		boolean connected = true;
		try {
			Scanner scanner = new Scanner(getSocket().getInputStream());
			while (connected) {
				try {
					String msg = scanner.useDelimiter(MSG_DELIMITER).next();
					if (msg.trim().startsWith("NEW ")) {
						// NEW <name> - Anmelden
						String name = msg.substring(4).trim();
						if (name.matches(NAME_PATTERN)) {
							synchronized ("add client") {
								if (server.addClient(name, clientAddress())) {
									respond("OK");
								} else {
									respond("ERROR name taken");
								}
							}
						} else {
							respond("ERROR name invalid");
						}
					} else if (msg.trim().startsWith("INFO")) {
						// INFO - Teilnehmerliste zurueckschicken.

						if (server.isIdentified(clientAddress())) {
							StringBuilder response = new StringBuilder();
							Map<InetAddress, String> clients = server
									.getClients();
							response.append("LIST");
							response.append(String.format(" %d", clients.size()));
							for (Map.Entry<InetAddress, String> e : clients
									.entrySet()) {
								String ip = e.getKey().toString();
								ip = ip.substring(ip.indexOf('/') + 1); // strip
																		// host
																		// name
								response.append(String.format(" %s %s", ip,
										e.getValue()));
							}
							respond(response.toString());
						} else {
							respond("ERROR not identified");
						}
					} else if (msg.trim().equals("BYE")) {
						// BYE - Abmelden
						respond("BYE");
						System.out.println(String.format(
								"%s requests end of connection", getSocket()
										.getInetAddress()));
						connected = false;
					} else {
						System.out.println(String.format(
								"ERROR \"%s\" is unknown", msg.trim()));
						respond(String.format("ERROR \"%s\" is unknown",
								msg.trim()));
					}
				} catch (NoSuchElementException e) {
					connected = false;
					System.out.println(String.format("Client %s offline?",
							socket.getInetAddress()));
				} catch (Exception e) {
					connected = false;
					e.printStackTrace();
				}
			}
			scanner.close();
			try {
				System.out.println(String.format("close connection for %s",
						socket.getInetAddress()));
				server.removeClient(clientAddress());
				socket.close();
				socket = null;
			} catch (IOException e) {
				System.out
						.println(String.format("Can't close connection to %s",
								socket.getInetAddress()));
			} finally {
				socket = null;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private Socket getSocket() {
		return socket;
	}

	private void respond(String msg) {
		try {
			getSocket().getOutputStream().write(
					(msg + MSG_DELIMITER).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public InetAddress clientAddress() {
		return socket.getInetAddress();
	}

}
