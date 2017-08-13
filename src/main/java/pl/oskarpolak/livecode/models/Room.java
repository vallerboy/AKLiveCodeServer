package pl.oskarpolak.livecode.models;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Lenovo on 24.07.2017.
 */
public class Room {
    private String name;
    private List<User> userList = new ArrayList<>();

    public Room(String name) {
        this.name = name;
    }

    public void addUser(User user){
        userList.add(user);
    }

    public void removeUser(User user){
        userList.remove(user);
    }

    public User getUserByName(String name){
        System.out.println("Name z getUserByName: " + name);
        return userList.stream()
                .filter(s -> s.getName().equals(name))
                .findAny().get();
    }

    public Optional<User> getUserBySessionId(String sessionId){
        return userList.stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findAny();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<User> getUserList() {
        return userList;
    }


}
