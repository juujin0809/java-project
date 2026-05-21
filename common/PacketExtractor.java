package chat;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PacketExtractor {

    public static String getPacketInfo(Socket socket, String message) {              // String message 부분에 채팅 역할 변수 삽입
        try {
            // 데이터 추출
            String ip = socket.getInetAddress().getHostAddress();
            int port = socket.getPort();
            
            // 바이트 크기 계산 
            int byteSize = message.getBytes(StandardCharsets.UTF_8).length;
            
            //  문자열 포맷팅
            return String.format("<html><font color='gray'>IP: <b>%s</b> | Port: %d | <i>%d Bytes</i></font></html>", ip, port, byteSize);
            
        } catch (Exception e) {
            // 에러 발생 시 프로그램 종료를 막기 위한 안전장치
            return "[네트워크 정보 추출 실패]";
        }
    }
}
// String packetData = PacketExtractor.getPacketInfo(senderSocket, chatMessage); GUI 넣을 때