package com.example.webchatserver;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.example.webchatserver.utils.ResourceAPI.loadChatRoomHistory;
import static com.example.webchatserver.utils.ResourceAPI.saveChatRoomHistory;


@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    //private static Map<String, String> roomHistoryList = new HashMap<String, String>();
    private static Map<String, ChatRoom> chatRooms = new HashMap<>();

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        ChatRoom chatRoom;
        if (chatRooms.containsKey(roomID)) {
            chatRoom = chatRooms.get(roomID);
            chatRoom.addUser(session.getId(), "");
        } else {
            chatRoom = new ChatRoom(roomID, session.getId());
            chatRooms.put(roomID, chatRoom);
        }

        // loading the history chat
        String history = loadChatRoomHistory(roomID);
        System.out.println("Room joined ");
        if (history != null && !(history.isBlank())) {
            System.out.println(history);
            history = history.replaceAll(System.lineSeparator(), "\\\n");
            System.out.println(history);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + history + " \\n Chat room history loaded\"}");
            //roomHistoryList.put(roomID, history + " \\n " + roomID + " room resumed.");
        }
//        if (!roomHistoryList.containsKey(roomID)) { // only if this room has no history yet
//            roomHistoryList.put(roomID, roomID + " room Created."); // initiating the room history
//        }

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome to the chat room. Please state your username to begin.\"}");
    }


    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        String roomID = null;
        ChatRoom chatRoom = null;

        for (Map.Entry<String, ChatRoom> entry : chatRooms.entrySet()) {
            if (entry.getValue().inRoom(userId)) {
                roomID = entry.getKey();
                chatRoom = entry.getValue();
                break;
            }
        }

        if (chatRoom != null && roomID != null) {
            String username = chatRoom.getUsers().get(userId);
            chatRoom.removeUser(userId);

            // adding event to the history of the room
//            String logHistory = roomHistoryList.get(roomID);
//            roomHistoryList.put(roomID, logHistory + " \\n " + username + " left the chat room.");

            // broadcasting it to peers in the same room
            int countPeers = 0;
            for (Session peer : session.getOpenSessions()) { // broadcast this person left the server
                if (chatRooms.get(roomID).inRoom(peer.getId())) { // broadcast only to those in the same room
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + username + " left the chat room.\"}");
                    countPeers++; // count how many peers are left in the room
                }
            }

            // if everyone in the room left, save history
//            if (!(countPeers > 0)) {
//                saveChatRoomHistory(roomID, roomHistoryList.get(roomID));
//            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {

        String userID = session.getId();
        String roomID = null;
        ChatRoom chatRoom = null;

        for (Map.Entry<String, ChatRoom> entry : chatRooms.entrySet()) {
            //Find the chatroom user is in
            System.out.println("CHATROOM MAP: " +  entry.getKey() + " " + entry.getValue());
                if (entry.getValue().inRoom(userID)) {
                roomID = entry.getKey();
                chatRoom = entry.getValue();

                System.out.println(roomID + " AHA AHA " + chatRoom);
                break;
            }
        }

        System.out.println("HANDLED IG" + chatRoom + " " + roomID + " " + userID);

        if (chatRoom == null) {
            return;
        }

        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

        if (chatRoom.getUsers().get(userID) != null && !chatRoom.getUsers().get(userID).isEmpty()) {
            System.out.println("pls tell me you enter");
            String username = chatRoom.getUsers().get(userID);

//            // adding event to the history of the room
//            String logHistory = roomHistoryList.get(roomID);
//            roomHistoryList.put(roomID, logHistory + " \\n " + "(" + username + "): " + message);

            // broadcasting it to peers in the same room
            for (Session peer : session.getOpenSessions()) {
                // only send my messages to those in the same room
                if (chatRooms.get(roomID).inRoom(peer.getId())) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message + "\"}");
                }
            }
        } else {
            chatRoom.setUserName(userID, message);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome, " + message + "!\"}");

//            // adding event to the history of the room
//            String logHistory = roomHistoryList.get(roomID);
//            roomHistoryList.put(roomID, logHistory + " \\n " + message + " joined the chat room.");

            // broadcasting it to peers in the same room
            for (Session peer : session.getOpenSessions()) {
                // only announce to those in the same room as me, excluding myself
                if ((!peer.getId().equals(userID)) && (chatRooms.get(roomID).inRoom(peer.getId()))) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + " joined the chat room.\"}");
                }
            }
        }
//        saveChatRoomHistory(roomID, roomHistoryList.get(roomID));
    }
}