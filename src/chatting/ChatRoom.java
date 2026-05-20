package chatting;

import java.util.ArrayList;

public class ChatRoom {

    // 채팅방 이름
    private String roomName;

    // 현재 방에 들어와 있는 사용자 목록
    private ArrayList<ClientHandler> users;

    // 생성자
    public ChatRoom(String roomName) {
        this.roomName = roomName;
        users = new ArrayList<>();
    }

    // 방 이름 반환
    public String getRoomName() {
        return roomName;
    }

    // 사용자 입장
    public void addUser(ClientHandler user) {

        users.add(user);

        broadcast("[알림] " + user.getNickname() + "님이 입장했습니다.");
    }

    // 사용자 퇴장
    public void removeUser(ClientHandler user) {

        users.remove(user);

        broadcast("[알림] " + user.getNickname() + "님이 퇴장했습니다.");
    }

    // 현재 방의 모든 사용자에게 메시지 전송
    public void broadcast(String message) {

        for(ClientHandler user : users) {
            user.sendMessage(message);
        }
    }
}