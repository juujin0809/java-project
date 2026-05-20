package components;

import java.util.HashMap;

public class UserDAO {

    private HashMap<String, String> userMap = new HashMap<>();

    public void register(String id, String pw) {
        userMap.put(id, pw);
    }

    public boolean exists(String id) {
        return userMap.containsKey(id);
    }

    public boolean login(String id, String pw) {
        return userMap.containsKey(id) && userMap.get(id).equals(pw);
    }
}