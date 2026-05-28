package common;

import java.io.PrintWriter;

public class WhisperHandler {
    
    // 서버에 메시지를 보내는 출력 스트림
    private PrintWriter out;
    
    // 내 닉네임 (보내는 사람)
    private String myNickname;
    
    // 생성자
    public WhisperHandler(PrintWriter out, String myNickname) {
        this.out = out;
        this.myNickname = myNickname;
    }
    
    /**
     * 귓속말 보내는 메서드
     * 서버의 규격(tokens.length >= 5)에 맞추기 위해 현재 방 이름을 패킷에 포함합니다.
     */
    public void sendWhisper(String currentRoom, String targetNickname, String message) {
        // 규격: WHISPER/방이름/보내는사람/받는사람/메시지내용
        String packet = Protocol.CHAT + Protocol.SEPARATOR // 또는 Protocol.WHISPER
                + currentRoom + Protocol.SEPARATOR 
                + myNickname + Protocol.SEPARATOR 
                + targetNickname + Protocol.SEPARATOR 
                + message;
        out.println(packet);
    }
    
    /**
     * 서버에서 전송된 귓속말 메시지인지 확인합니다.
     * ChatServer는 귓속말을 전송할 때 "[귓속말]" 이라는 텍스트를 붙여서 보냅니다.
     */
    public boolean isWhisper(String serverMessage) {
        return serverMessage != null && serverMessage.startsWith("[귓속말]");
    }
    
    /**
     * 서버가 보낸 "[귓속말] sender → receiver: content" 형태에서 보낸 사람을 추출합니다.
     */
    public String getSender(String serverMessage) {
        try {
            // "[귓속말] " 뒤에서부터 " →" 전까지 자르기
            int start = serverMessage.indexOf("] ") + 2;
            int end = serverMessage.indexOf(" →");
            if (start >= 0 && end > start) {
                return serverMessage.substring(start, end).trim();
            }
        } catch (Exception e) {
            System.out.println("보낸사람 추출 실패: " + e.getMessage());
        }
        return "Unknown";
    }
    
    /**
     * 서버가 보낸 "[귓속말] sender → receiver: content" 형태에서 본문 내용을 추출합니다.
     */
    public String getContent(String serverMessage) {
        try {
            // 첫 번째 ": " 뒤의 모든 내용을 본문으로 취급
            int start = serverMessage.indexOf(": ");
            if (start >= 0) {
                return serverMessage.substring(start + 2);
            }
        } catch (Exception e) {
            System.out.println("메시지 내용 추출 실패: " + e.getMessage());
        }
        return serverMessage;
    }
}
