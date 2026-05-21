package chatting;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private Socket socket;

    private BufferedReader in;
    private PrintWriter out;

    private String nickname;

    // 현재 들어가 있는 채팅방
    private ChatRoom currentRoom;

    // 생성자
    public ClientHandler(Socket socket) {

        this.socket = socket;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // 닉네임 반환
    public String getNickname() {
        return nickname;
    }

    // 메시지 전송
    public void sendMessage(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
     
    	try {
        	
            // 최초 접속 시 닉네임 입력
            nickname = in.readLine();

            sendMessage("환영합니다, " + nickname + "님!");

            // 기본 채팅방 입장
            currentRoom = ChatServer.room;  // 채팅 서버 이름을 일단 임의로 'ChatServer'라고 함.

            currentRoom.addUser(this);

            String msg;

            // 메시지 계속 수신
            while((msg = in.readLine()) != null) {

                // 채팅방 나가기
                if(msg.equals("/exit")) {

                    currentRoom.removeUser(this);

                    sendMessage("채팅방에서 퇴장했습니다.");

                    break;
                }

                // 일반 메시지
                currentRoom.broadcast("[" + nickname + "] " + msg);
            }

        } catch(Exception e) {

            System.out.println("클라이언트 연결 종료");

        } finally {

            try {
                socket.close();
            } catch(Exception e) {}
        }
    }
}