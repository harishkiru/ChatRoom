package com.example.webchatserver;

import jakarta.websocket.Session;

import java.util.*;

public class ChatRoom {
    private String code;

    // each user has a unique ID associated with their WebSocket session and their username
    // <userID, username>
    private Map<String, String> users = new HashMap<String, String>();
    // all the sessions in the room
    private Set<Session> sessions = new HashSet<>();
    // the roomID is the same as the room code
    String roomID;

    // when created, the chat room has at least one user
    public ChatRoom(String code, String user, Session session) {
        // the room code
        this.code = code;
        // the user's ID and username
        users.put(user, "");

        // When created, the user has not entered their username yet
        // add the user to the room
        addUser(user, "", session);
    }



    // set the room code
    public void setCode(String code) {
        this.code = code;
    }
    // set the room ID
    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }
    // get the room ID
    public String getRoomID() {
        return this.roomID;
    }
    // get the room name
    public String getRoomName() {
        return this.code;
    }
    // get the users in the room
    public Map<String, String> getUsers() {
        return users;
    }

    public void addUser(String userID, String name, Session session) {
        // add the user to the room
        users.put(userID, name);
        // add the session to the room
        sessions.add(session);
    }

    public boolean inRoom(String userID) {
        return users.containsKey(userID);
    }
    // check if the room is empty
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
    // get the sessions in the room
    public Set<Session> getSessions() {
        return sessions;
    }
}
