package chat.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class Controller {
	private Chat view;
	private Client client;
	private Thread userUpdater;
	
	public Controller() {
		userUpdater = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
						updateClients();
					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						System.err.println("Most likely a Client closed his connection");
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void setView(Chat view) {
		this.view = view;
	}
	
	public void connect(String server, String nick) {
		try {
			client = new Client(InetAddress.getByName(server), new MessageReceiver() {
			    @Override
			    public void receiveMessage(String msg) {
			        messageRecived(msg);
			    }
			});
			
			if(!client.setName(nick)){
				throw new IOException("Wrong Username");
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		userUpdater.start();
	}
	
	protected void updateClients() {
		Map<String, InetAddress> clients = client.getClients();
		StringBuilder list = new StringBuilder();
		for (Map.Entry<String, InetAddress> e : clients.entrySet()) {
			list.append(e.getKey()+"\n");
		}
		view.getNicks().setText(list.toString());
	}

	public void messageRecived(String message) {
		String old = view.getMessages().getText();
		view.getMessages().setText(old + message);
	}
	
	public void sendMessage(String message) {
		client.sendMessage(message);
	}

	public void run() {
		view.getFrame().setVisible(true);
	}
	
	public void close() {
		client.disconnect();
		userUpdater.interrupt();
		System.exit(0);
	}
}
