package networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JOptionPane;

public class Multiplayer implements Runnable {
	
	private User user;
	private User opponent;
	private Socket socket;
	private ServerSocket server;
	
	public Multiplayer(User u, Socket s) {
		user = u;
		socket = s;
		
		Thread t = new Thread(this);
		t.start();
		boolean lobby = true;
		do {
			String onlinePlayers = Server.SendMessage(socket, "GET_LIST||"+user.Username+"||");
			String[] players = onlinePlayers.split("\\|\\|");
			String opponentName = (String) JOptionPane.showInputDialog(null, "Select an Opponent to Challenge", user.Username, JOptionPane.QUESTION_MESSAGE, null, players, null);
			if(opponentName==null) {
				if(opponent==null) {
					logout();
				}else {
					return;
				}
			} else if(opponentName.equals("Refresh")) { 
				lobby = true;
			}else {
				 lobby = !challenge(opponentName);
			}
		}while(lobby);
		new Chess(user, opponent, true);
	}
	
	
	
	public void logout() {
		String response = Server.SendMessage(socket, "LOGOUT||"+user.Username+"||");
		if(response.equals("OK")) {
			JOptionPane.showMessageDialog(null, "Logout Successful");
			System.exit(0);
		}
		JOptionPane.showMessageDialog(null, response);
		System.exit(1);
	}
	
	public boolean challenge(String opponentName) {
		int n = JOptionPane.showConfirmDialog(
                null, "Do you want to challenge " + opponentName, "Challenge",
                JOptionPane.YES_NO_OPTION);
		if(n==0) {
			String response[] = Server.SendMessage(socket, "GET_USER||" + opponentName + "||").split("\\|\\|");
			opponent = new User(response[0],Integer.parseInt(response[1]),response[2]);
			String challengeResponse = Server.SendMessage(opponent.socket, "CHALLENGE||"+user.Username+"||"+opponentName+"||");
			if(challengeResponse!=null && challengeResponse.equals("OK")) {
				return true;
			}else {
				JOptionPane.showMessageDialog(null, challengeResponse);
			}
		}
		return false;
	}
	
	
	
	public String parseCommand(String command, Socket connection) {
		String data[] = command.split("\\|\\|");
		switch(data[0]) {
		case "CHALLENGE":
			int n = JOptionPane.showConfirmDialog(
                    null, data[1] + " wants to challenge you. Do you accept?", "Challenge",
                    JOptionPane.YES_NO_OPTION);
			if(n==0) {
				opponent = new User(data[1], connection);
				return "OK";
			}
			return "Challenge Declined!";
		}
		return null;
	}
	
	
	@Override
	public void run() {
		listen();
	}
	public void listen() {
		while(true) {
			try {
				Socket connection = server.accept();
	            Scanner input = new Scanner(connection.getInputStream());
	            String command = input.nextLine();
	            input.close();
	            String response = parseCommand(command, connection);
	            
	            if(command.contains("CHALLENGE") && response.contains("OK")) {
	            	new Chess(user, opponent, false);
	                socket.close();
	                break;
	            }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
