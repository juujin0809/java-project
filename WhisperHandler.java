package common;


import java.io.PrintWriter;

public class WhisperHandler {
    
    // 서버에 메시지를 보내는 출력 스트림
    private PrintWriter out;
    
    // 내 아이디 (보내는 사람)
    private String myId;
    
    // 생성자
    public WhisperHandler(PrintWriter out, String myId) {
        this.out = out;
        this.myId = myId;
    }
    
    // 귓속말 보내는 메서드
    public void sendWhisper(String targetId, String message) {
        String packet = Protocol.WHISPER 
                + Protocol.SEPARATOR + myId 
                + Protocol.SEPARATOR + targetId 
                + Protocol.SEPARATOR + message;
        out.println(packet);
    }
    
    // 받은 메시지가 귓속말인지 확인
    public boolean isWhisper(String message) {
        return message.startsWith(Protocol.WHISPER);
    }
    
    // 보낸사람 추출
    public String getSender(String message) {
        String[] parts = message.split(Protocol.SEPARATOR);
        return parts[1];
    }
    
    // 메시지 내용 추출
    public String getContent(String message) {
        String[] parts = message.split(Protocol.SEPARATOR);
        return parts[3];
    }
}
