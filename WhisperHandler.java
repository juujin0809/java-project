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
    
    // 귓속말 보내는 메서드 (/w 상대닉네임 내용 형식)
    public void sendWhisper(String targetNickname, String message) {
        String packet = Protocol.WHISPER 
                + Protocol.SEPARATOR + myNickname 
                + Protocol.SEPARATOR + targetNickname 
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
