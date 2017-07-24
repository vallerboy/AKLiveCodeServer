package pl.oskarpolak.livecode.models;

import org.springframework.web.socket.WebSocketSession;

/**
 * Created by Lenovo on 24.07.2017.
 */
public class User {
    private String name;
    private WebSocketSession session;
    private String sessionId;

    public User(WebSocketSession session) {
        this.session = session;
        sessionId = session.getId();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }
}
