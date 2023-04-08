package com.example.webchatserver;

import jakarta.websocket.Session;

import java.util.*;

public class ChatRoom {
    private String code;

    // each user has a unique ID associated with their WebSocket session and their username
    private Map<String, String> users = new HashMap<String, String>();
    private Set<Session> sessions = new HashSet<>();
    String roomID;

    // when created, the chat room has at least one user
    public ChatRoom(String code, String user, Session session) {
        this.code = code;
        users.put(user, "");

        // When created, the user has not entered their username yet
        addUser(user, "", session);
    }
    


    public void setCode(String code) {
        this.code = code;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }

    public String getRoomID() {
        return this.roomID;
    }

    public String getRoomName() {
        return this.code;
    }

    public Map<String, String> getUsers() {
        return users;
    }

    public void addUser(String userID, String name, Session session) {
        users.put(userID, name);
        sessions.add(session);
    }

    public boolean inRoom(String userID) {
        return users.containsKey(userID);
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    public void setUserName(String userID, String name, Session session) {
        if (users.containsKey(userID)) {
            // Update the user's name
            users.put(userID, name);
        } else {
            // Add a new user with the given name
            addUser(userID, name, session);
        }
    }


    public void removeUser(String userID) {
        users.remove(userID);
    }

    public Set<Session> getSessions() {
        return sessions;
    }
}
