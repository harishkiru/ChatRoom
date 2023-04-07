package com.example.webchatserver;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.webchatserver.utils.ResourceAPI.loadChatRoomHistory;
import static com.example.webchatserver.utils.ResourceAPI.saveChatRoomHistory;

@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    private static Map<String, String> roomHistoryList = new HashMap<String, String>();
    private static Map<String, ChatRoom> chatRooms = new HashMap<>();
    public static List<String> activeChatRooms = new ArrayList<>(); // Moved to ChatServer


    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        joinRoom(roomID, session);
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        leaveRoom(session);
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
        handleUserMessage(comm, session);
    }

    private void joinRoom(String roomID, Session session) throws IOException, EncodeException {
        ChatRoom chatRoom = getOrCreateChatRoom(roomID, session);
        addUserToChatRoom(chatRoom, session);

        String history = loadChatRoomHistory(roomID);
        sendChatHistory(history, session, roomID);

        if (!roomHistoryList.containsKey(roomID)) {
            roomHistoryList.put(roomID, "\\n" + roomID + " room Created.~S~");
        }

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome to the chat room. Please state your username to begin.\"}");

        activeChatRooms.add(roomID); // Update the activeChatRooms in ChatServer
    }

    private ChatRoom getOrCreateChatRoom(String roomID, Session session) {
        ChatRoom chatRoom;
        if (chatRooms.containsKey(roomID)) {
            chatRoom = chatRooms.get(roomID);
        } else {
            chatRoom = new ChatRoom(roomID, session.getId());
            chatRooms.put(roomID, chatRoom);
        }
        return chatRoom;
    }

    private void addUserToChatRoom(ChatRoom chatRoom, Session session) {
        chatRoom.addUser(session.getId(), "");
    }

    private void sendChatHistory(String history, Session session, String roomID) throws IOException, EncodeException {
        if (history != null && !(history.isBlank())) {
            String arr[] = history.split("~S~");
            for (String message : arr) {
                session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
            }
            roomHistoryList.put(roomID, history + " \\n " + roomID + " room resumed.~S~");
        }
    }

    private void leaveRoom(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        ChatRoom chatRoom = findChatRoomByUserId(userId);

        if (chatRoom != null) {
            String roomID = chatRoom.getRoomID();
            String username = chatRoom.getUsers().get(userId);
            chatRoom.removeUser(userId);

            if (chatRoom.isEmpty()) {
                saveChatRoomHistory(roomID, roomHistoryList.get(roomID));
                activeChatRooms.remove(roomID); // Update the activeChatRooms in ChatServer
            }

            updateRoomHistory(roomID, username + " left the chat room.");
            broadcastMessageToPeersInRoom(chatRoom, session, "(Server): " + username + " left the chat room.");
        }
    }

    private ChatRoom findChatRoomByUserId(String userId) {
        for (ChatRoom chatRoom : chatRooms.values()) {
            if (chatRoom.inRoom(userId)) {
                System.out.println(chatRoom.getRoomID() + " TGHIS IS THE CHAT ROOM ID");
                return chatRoom;
            }
        }
        return null;
    }

    private void updateRoomHistory(String roomID, String message) {
        String logHistory = roomHistoryList.get(roomID);
        roomHistoryList.put(roomID, logHistory + "\\n" + message);
    }

    private void broadcastMessageToPeersInRoom(ChatRoom chatRoom, Session session, String message) throws IOException, EncodeException {
        for (Session peer : session.getOpenSessions()) {
            if (chatRoom.inRoom(peer.getId())) {
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
            }
        }
    }

    private void handleUserMessage(String comm, Session session) throws IOException, EncodeException {
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

        String userID = session.getId();
        ChatRoom chatRoom = findChatRoomByUserId(userID);

        if (chatRoom == null) {
            return;
        }

        if (chatRoom.getUsers().get(userID) != null && !chatRoom.getUsers().get(userID).isEmpty()) {
            handleChatMessage(chatRoom, session, userID, message);
        } else {
            handleUsernameMessage(chatRoom, session, userID, message);
        }

        String roomID = chatRoom.getRoomID();
        saveChatRoomHistory(roomID, roomHistoryList.get(roomID));
    }

    private void handleChatMessage(ChatRoom chatRoom, Session session, String userID, String message) throws IOException, EncodeException {
        String roomID = chatRoom.getRoomID();
        String username = chatRoom.getUsers().get(userID);

        updateRoomHistory(roomID, "(" + username + "): " + message + "~S~");
        broadcastMessageToPeersInRoom(chatRoom, session, "(" + username + "): " + message);
    }

    private void handleUsernameMessage(ChatRoom chatRoom, Session session, String userID, String message) throws IOException, EncodeException {
        chatRoom.setUserName(userID, message);

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome, " + message + "!\"}");

        String roomID = chatRoom.getRoomID();
        updateRoomHistory(roomID, message + " joined the chat room.");
        broadcastMessageToPeersInRoom(chatRoom, session, "(Server): " + message + " joined the chat room.");
    }

    //Function to get the usernames of the users in a chatroom
    public static List<String> getUsernames(String roomID) {
        List<String> usernames = new ArrayList<>();
        ChatRoom chatRoom = chatRooms.get(roomID);
        for (String userID : chatRoom.getUsers().keySet()) {
            usernames.add(chatRoom.getUsers().get(userID));
        }
        return usernames;
    }
}
