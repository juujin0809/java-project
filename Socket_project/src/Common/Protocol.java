package Common;

public class Protocol {
    // 일반 채팅 메시지 타입
    public static final String CHAT = "CHAT";

    // 귓속말 메시지 타입 (특정 1명에게만 전송)
    public static final String WHISPER = "WHISPER";

    // 메시지 구분자
    // 형식: 타입/보내는사람/받는사람/내용
    public static final String SEPARATOR = "/";
}
