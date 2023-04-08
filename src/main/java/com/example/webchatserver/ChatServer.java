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

@ServerEndpoint(value="/ws/{roomName}")
public class ChatServer {

    private static Map<String, String> roomHistoryList = new HashMap<String, String>();
    private static Map<String, ChatRoom> chatRooms = new HashMap<>();
    public static List<String> activeChatRooms = new ArrayList<>();

    @OnOpen
    public void open(@PathParam("roomName") String roomName, Session session) throws IOException, EncodeException {
        joinRoom(roomName, session);
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        leaveRoom(session);
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
        handleUserMessage(comm, session);
    }

    private void joinRoom(String roomName, Session session) throws IOException, EncodeException {
        ChatRoom chatRoom = getOrCreateChatRoom(roomName, session);
        addUserToChatRoom(chatRoom, session);

        String history = loadChatRoomHistory(roomName);
        sendChatHistory(history, session, roomName);

        if (!roomHistoryList.containsKey(roomName)) {
            roomHistoryList.put(roomName, "\\n" + roomName + " room Created.~S~");
        }

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome to the chat room. Please state your username to begin.\"}");

        if (!activeChatRooms.contains(roomName)) {
            activeChatRooms.add(roomName);
        }
    }

    private String generateUniqueroomName() {
        return ChatServlet.generatingRandomUpperAlphanumericString(5);
    }


    private ChatRoom getOrCreateChatRoom(String roomName, Session session) {
        ChatRoom chatRoom = null;
        for (ChatRoom existingChatRoom : chatRooms.values()) {
            if (existingChatRoom.getRoomName().equals(roomName)) {
                chatRoom = existingChatRoom;
                break;
            }
        }

        if (chatRoom == null) {
            chatRoom = new ChatRoom(roomName, session.getId(), session);
            chatRoom.setRoomID(generateUniqueroomName());
            chatRooms.put(chatRoom.getRoomID(), chatRoom);
        }
        return chatRoom;
    }


    private void addUserToChatRoom(ChatRoom chatRoom, Session session) {
        chatRoom.addUser(session.getId(), "", session);
    }

    private void sendChatHistory(String history, Session session, String roomName) throws IOException, EncodeException {
        if (history != null && !(history.isBlank())) {
            String arr[] = history.split("~S~");
            for (String message : arr) {
                session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
            }
            roomHistoryList.put(roomName, history + " \\n " + roomName + " room resumed.~S~");
        }
    }

    private void leaveRoom(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        ChatRoom chatRoom = findChatRoomByUserId(userId);

        if (chatRoom != null) {
            String roomName = chatRoom.getRoomName();
            String username = chatRoom.getUsers().get(userId);
            chatRoom.removeUser(userId);

            if (chatRoom.isEmpty()) {
                saveChatRoomHistory(roomName, roomHistoryList.get(roomName));
                activeChatRooms.remove(roomName);
                chatRooms.remove(chatRoom.getRoomID()); // Remove the chat room from the map
            }

            updateRoomHistory(roomName, username + " left the chat room.");
            goodbyeBroadcast(chatRoom, session, "(Server): " + username + " left the chat room.");

            // Remove the session from the chat room
            chatRoom.getSessions().remove(session);
        }
    }


    private ChatRoom findChatRoomByUserId(String userId) {
        for (ChatRoom chatRoom : chatRooms.values()) {
            if (chatRoom.inRoom(userId)) {
                return chatRoom;
            }
        }
        return null;
    }

    private void updateRoomHistory(String roomName, String message) {
        String logHistory = roomHistoryList.get(roomName);
        roomHistoryList.put(roomName, logHistory + "\\n" + message);
    }

    private void broadcastMessageToPeersInRoom(ChatRoom chatRoom, Session session, String message) throws IOException, EncodeException {
        for (Session peer : session.getOpenSessions()) {
            if (chatRoom.inRoom(peer.getId())) {
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
            }
        }
    }

    private void welcomeBroadcast(ChatRoom chatRoom, Session session, String message) throws IOException, EncodeException {
        for (Session peer : session.getOpenSessions()) {
            if (chatRoom.inRoom(peer.getId())) {
                peer.getBasicRemote().sendText("{\"type\": \"userJoin\", \"message\":\"" + message + "\"}");
            }
        }
    }

    private void goodbyeBroadcast(ChatRoom chatRoom, Session session, String message) throws IOException, EncodeException {
        for (Session peer : session.getOpenSessions()) {
            if (chatRoom.inRoom(peer.getId())) {
                peer.getBasicRemote().sendText("{\"type\": \"userLeave\", \"message\":\"" + message + "\"}");
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

        String roomName = chatRoom.getRoomName();
        saveChatRoomHistory(roomName, roomHistoryList.get(roomName));
    }

    private void handleChatMessage(ChatRoom chatRoom, Session session, String userID, String message) throws IOException, EncodeException {
        String roomName = chatRoom.getRoomName();
        String username = chatRoom.getUsers().get(userID);

        updateRoomHistory(roomName, "(" + username + "): " + message + "~S~");
        broadcastMessageToPeersInRoom(chatRoom, session, "(" + username + "): " + message);
    }

    private void handleUsernameMessage(ChatRoom chatRoom, Session session, String userID, String message) throws IOException, EncodeException {
        chatRoom.setUserName(userID, message, session);

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome, " + message + "!\"}");

        String roomName = chatRoom.getRoomName();
        updateRoomHistory(roomName, message + " joined the chat room.");
        welcomeBroadcast(chatRoom, session, "(Server): " + message + " joined the chat room.");
    }

    //Function to get the usernames of the users in a chatroom
    public static List<String> getUsernames(String roomName) {
        List<String> usernames = new ArrayList<>();
        ChatRoom targetChatRoom = null;
        for (ChatRoom chatRoom : chatRooms.values()) {
            if (chatRoom.getRoomName().equals(roomName)) {
                targetChatRoom = chatRoom;
                break;
            }
        }

        if (targetChatRoom != null) {
            for (String userID : targetChatRoom.getUsers().keySet()) {
                // Don't put UUID in the list
                if (targetChatRoom.getUsers().get(userID).length() > 25) {
                    continue;
                }
                usernames.add(targetChatRoom.getUsers().get(userID));
                System.out.println(targetChatRoom.getUsers().get(userID) + " THIS IS THE USERNAME" + targetChatRoom.getUsers().keySet() + " THIS IS THE KEYSET");
            }
        }
        return usernames;
    }

}
