public class Protocol {
    // 일반 채팅 메시지 타입
    public static final String CHAT = "CHAT";
    
    // 귓속말 메시지 타입 (특정 1명에게만 전송)
    public static final String WHISPER = "WHISPER";
    
    // ⭐ 메시지 구분자 (기존 "/" 대신 본문 슬래시 충돌을 방지하기 위해 안전한 기호 조합 사용)
    public static final String SEPARATOR = "◀▶";
}
