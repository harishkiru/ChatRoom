package com.example.webchatserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the data you may need to store about a Chat room
 * You may add more method or attributes as needed
 * **/
public class ChatRoom {
    private String code;

    //each user has an unique ID associate to their ws session and their username
    private  Map<String, String> users = new HashMap<String, String>();

    // when created the chat room has at least one user
    public ChatRoom(String code, String user){
        this.code = code;
        users.put(code, user);


        // When created, the user has not entered their username yet
        this.users.put(user, "");
    }
    public void setCode(String code) {
        this.code = code;
    }

    public String getRoomID() {
        return this.code;
    }

    public Map<String, String> getUsers() {
        return users;
    }

    public void addUser(String userID, String name){
        users.put(userID, name);
    }

    public boolean inRoom(String userID){
        return users.containsKey(userID);
    }

    public boolean isEmpty() { return users.isEmpty(); }
    /**
     * This method will add the new userID to the room if not exists, or it will add a new userID,name pair
     * **/
    public void setUserName(String userID, String name) {
        // update the name
        if(users.containsKey(userID)){
            users.remove(userID);
            users.put(userID, name);
        }else{ // add new user
            users.put(userID, name);
        }
    }

    /**
     * This method will remove a user from this room
     * **/
    public void removeUser(String userID){
        if(users.containsKey(userID)){
            users.remove(userID);
        }
    }

    //Get all the users in a chatroom
    public List<String> getUsersInRoom() {
        List<String> usersInRoom = new ArrayList<String>();
        for (Map.Entry<String, String> entry : users.entrySet()) {
            usersInRoom.add(entry.getValue());
        }
        return usersInRoom;
    }


}
