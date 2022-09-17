
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class Server {
	public HashMap<String, DataOutputStream> clients; // 클라이언트 정보 저장
	public HashMap<String, String> clients_job; // 클라이언트 직업 정보 저장
	boolean day = true;
	String max;
	Iterator<String> it;
	
	public static void main(String[] args) { // 서버 시작
		 Server server = new Server();
		 server.Start(); 
	}
	
	public void Start() { // 클라이언트를 서버와 연결, 클라이언트로부터 입력된 내용 받아오기, 게임 시작
		clients = new HashMap<String, DataOutputStream>();
		clients_job = new HashMap<String, String>();
		ServerSocket serverSocket = null;
		Socket socket = null;

		try {
			serverSocket = new ServerSocket(10002);
			System.out.println("###마피아 게임### (3명 이상 접속 시 시작합니다.)");
			GameStart start = new GameStart();
			
			while (true) {
				socket = serverSocket.accept(); // 클라이언트 연결요청 기다림
				ServerReceiver receiver = new ServerReceiver(socket);
				receiver.start();
			} //while
		} catch (Exception e) {
			e.printStackTrace();
		}
	} //start
	
	void sendToAll(String msg, String id) { // 모두에게 메시지를 보여줌
		Iterator<String> it = clients.keySet().iterator();
		
		while(it.hasNext()) {
			String tmpId = it.next(); 
			try {
				DataOutputStream out = (DataOutputStream) clients.get(tmpId); // 클라이언트 이름 저장
				if (out.equals(clients.get(id)))
					continue; // 메세지 보낸 본인 화면에는 출력 안함
				out.writeUTF(msg); // 입력한 메시지 출력
			} catch (IOException e) {
				e.printStackTrace();
			} //catch
		} //while
	} //sendToAll
	
	void sendTo(String msg, String id) { // 특정인에게만 출력
		Iterator<String> it = clients.keySet().iterator();
		
		while(it.hasNext()) {
			String tmpId = it.next();
			try {
				DataOutputStream out = (DataOutputStream) clients.get(tmpId);
				if (!out.equals(clients.get(id))) // 특정인이 아닐시 출력 안함
					continue; 
				out.writeUTF(msg); // 입력한 메시지 출력
			} catch (IOException e) {
				e.printStackTrace();
			}
		} //while
	} //sendTo
	
	class ServerReceiver extends Thread{ // 
		Socket socket;
		DataInputStream in;
		DataOutputStream out;
		Client client; // 클라이언트 객체 받음
		String id; // Clients의 key값
		
		ServerReceiver(Socket socket) { 
			this.socket = socket;
			try {
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			try {
				id = in.readUTF(); // 클라이언트 이름 입력받아옴
				clients.put(id, out); // 입력받은 이름을 해시맵에 저장
				clients_job.put(id, "Citizen"); // 입력받은 이름을 해시맵에 저장 후 시민으로 초기화
				sendToAll("[" + id + "]님이 들어오셨습니다.", "Server"); // 서버화면 제외하고 클라이언트에게 입장 메시지 출력
				System.out.println("현재 접속자 수는 " + clients.size() + "입니다."); 
				
				GameStart start = new GameStart();
				if(clients.size() >= 3) { // 3명 이상 입장 시 실행
					shuffle(); // 마피아 설정
					it = clients_job.keySet().iterator();
					while (it.hasNext()) { // 클라이언트 정보를 본인에게 출력
						String temp = it.next();
						sendTo("\n=== 내 정보 ===",temp);
						sendTo("이름 : " + temp,temp);
						sendTo("직업 : " + clients_job.get(temp),temp);
					}
					start.start(); // 게임 시작 (GameStart)
				}
				
				while(in!=null) {
					String msg = in.readUTF(); // 클라이언트가 보낸 메세지
					sendToAll(msg, id); // 받은 메세지를 클라이언트 모두에게 출력
					System.out.println(msg); // 서버에서 메세지 확인
				} //while
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				while(in!=null) {
					String msg = in.readUTF(); // 클라이언트가 보낸 메세지
					sendToAll(msg, id); // 받은 메세지를 클라이언트 모두에게 출력
					System.out.println(msg); // 서버에서 메세지 확인
				} 
			} catch (IOException e) {
				e.printStackTrace();
			} finally { // 클라이언트가 퇴장 시
				sendToAll("[" + id + "]님이 나가셨습니다.", "Server");
				clients.remove(id);
				System.out.println("현재 접속자 수는 " + clients.size() + "입니다.");
			} //finally
			
		} //run
	
	} //receiver
	
	public class GameStart extends Thread {
		boolean Win() {
			boolean win = true; // true : 시민 승
			int maf_num = 0, citi_num = 0;
			Iterator<String> it = clients_job.keySet().iterator();
			while (it.hasNext()) { // 마피아와 시민의 숫자 카운팅
				String tmpid = it.next();
				switch (tmpid) {
				case "Mafia":
					maf_num++;
					break;
				case "Citizen":
					citi_num++;
					break;
				} //switch
			} //while
			
			if (maf_num >= citi_num) { // 마피아의 수가 시민 보다 많거나 같을 시 : 마피아  승
				System.out.println("마피아의 승리");
				win = false;
			} else if (maf_num == 0) { // 마피아가 다 죽었을 시 : 시민 승
				System.out.println("시민의 승리");
				win = true;
			}
			return win;
		} //win
		
		public void run() { 
			while (day) { // 낮
				if (Win())
					break; // 승부 나면 종료
				try {
					System.out.println("낮이 되었습니다. 시민은 투표를 통해 누구를 죽일지 결정해주세요.");
					while(it.hasNext()) { // 10초 동안 투표받기
						String id = it.next();
						Vote(id);
					}
					Thread.sleep(10000); 
					day = false;
					
				if (!day) { // 밤
					System.out.println("밤이 되었습니다. 마피아는 죽일 시민을 결정해주세요.");
					while(it.hasNext()) { // 10초 동안 결정
						String id = it.next();
						Kill(id);
					}
					Thread.sleep(10000);
					day = true;
				} // 밤
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} // 낮 while
		} // run
	} //gamestart
	
	public void shuffle() { // Mafia 뽑기
		int num;
		num = clients.size();
		HashMap<String,Integer> tmpJob = new HashMap<String,Integer>(); // 직업 저장할 임시 맵
		
		for ( String key : clients.keySet() ) { // 임시 맵에 clients 키값 복사 후 랜덤하게 숫자 부여
			tmpJob.put(key,(int)(Math.random()*num+1));
		}
		
		// 부여한 숫자 중 최대값을 가진 클라이언트에게 마피아 지정
		clients_job.put( 
				Collections.max(tmpJob.entrySet(), 
						(entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey(),"Mafia");
		
	} //shuffle

	public void Vote(String id) { // 투표
		
		Scanner scanner = new Scanner(System.in);
		String message = "";
		String getMax;
		
		HashMap<String,Integer> tmp = new HashMap<String,Integer>(); // 득표 수 임시 맵
		
		for ( String key : clients.keySet() ) { // 임시 맵에 clients 키값 복사 후 0 부여
			tmp.put(key,0);
		}
		while(it.hasNext()) { // 입력받은 클라이언트의 이름과 동일 시 value 카운팅
			DataOutputStream out = (DataOutputStream) clients.get(id);
			if (out.equals(id)) {
				tmp.put(id, tmp.get(id) + 1);
			}
			
			// 카운팅 된 value 중 가장 큰 값을 갖는 사람 설정
			getMax = Collections.max(tmp.entrySet(), 
					(entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();
			
			max = getMax;
			} //while
		clients.remove(max);
		System.out.println(max+"님을 죽였습니다.");
		System.out.println("현재 접속자 수는 " + clients.size() + "입니다.");
	} //vote
		
	public void Kill(String id) { // 마피아가 투표
		Scanner scanner = new Scanner(System.in);
		String message = "";
		
		while (true) {
			DataOutputStream out = (DataOutputStream) clients.get(id);
			if (out.equals(id)) { // 입력받은 이름이 있을 시 해당 사람 퇴장
				sendTo(id+"님을 죽였습니다.","Mafia");
				System.out.println(id+"님이 죽었습니다.");
				clients.remove(id);
				System.out.println("현재 접속자 수는 " + clients.size() + "입니다.");
				break;
			} //if
		} //while
	} //kill

} //class


