import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	public static void main(String[] args) {  // 이름 입력 받고 시작
		try {
		InetAddress localAddress = InetAddress.getLocalHost();
		Scanner sc = new Scanner(System.in);
		
		System.out.println("닉네임을 입력하세요.");
		String id = sc.next();
		Client client = new Client();
		client.Start(id, localAddress);
		
	} catch (Exception e) {
		e.printStackTrace();
	}
}
	public void Start(String id, InetAddress localAddress) {
		try {
			
			Socket socket = new Socket(localAddress, 10002);
			System.out.println("서버에 연결 되었습니다.");
			
			Thread sender = new Thread(new ClientSender(socket, id));
			Thread receiver = new Thread(new ClientReceiver(socket));
			
			sender.start(); 
			receiver.start();
		} catch (ConnectException ce) {
			ce.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static class ClientSender extends Thread { // 입력받은 메시지 출력
		Socket socket;
		DataOutputStream out;
		String id;

		public ClientSender(Socket socket, String id) {
			this.socket = socket;
			try {
				out = new DataOutputStream(socket.getOutputStream());
				this.id = id;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void run() {
			Scanner sc = new Scanner(System.in);
			String msg = "";
			try {
				if (out != null) {
					out.writeUTF(id);
				}
				
				while (out != null) {
					msg = sc.nextLine();
					if (msg.isEmpty()) {// 아무것도 입력안하면 건너뜀
						continue;
					}
					else {
						out.writeUTF("[" + id + "] : " + msg);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // run()
	}

	public static class ClientReceiver extends Thread {
		Socket socket;
		DataInputStream in;

		public ClientReceiver(Socket socket) {
			this.socket = socket;
			try {
				in = new DataInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() { 
			
			while (in != null) {
				try {
					System.out.println(in.readUTF());
				} catch (IOException e) {
					System.out.print("서버와의 연결이 끊겼습니다.");
					System.exit(0);
				}
			}
		} // run
	}

}
