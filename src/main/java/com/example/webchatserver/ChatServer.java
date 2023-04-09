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
    // <roomName, history>
    private static Map<String, String> roomHistoryList = new HashMap<String, String>();
    // <roomID, chatRoom>
    private static Map<String, ChatRoom> chatRooms = new HashMap<>();
    //   list of active rooms
    public static List<String> activeChatRooms = new ArrayList<>();

    @OnOpen
    public void open(@PathParam("roomName") String roomName, Session session) throws IOException, EncodeException {
        // join the room
        joinRoom(roomName, session);
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        // leave the room
        leaveRoom(session);
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
        // handle the user message
        handleUserMessage(comm, session);
    }

    private void joinRoom(String roomName, Session session) throws IOException, EncodeException {
        // get the chat room
        ChatRoom chatRoom = getOrCreateChatRoom(roomName, session);
        // add the user to the chat room
        addUserToChatRoom(chatRoom, session);
// load the chat room history
        String history = loadChatRoomHistory(roomName);
        // send the chat room history
        sendChatHistory(history, session, roomName);

        if (!roomHistoryList.containsKey(roomName)) {
            // add the room to the history list
            roomHistoryList.put(roomName, "\\n" + roomName + " room Created.~S~");
        }

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome to the chat room. Please state your username to begin.\"}");// send a welcome message to the user

        if (!activeChatRooms.contains(roomName)) {
            // add the room to the active rooms list
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
        // add the user to the chat room
        chatRoom.addUser(session.getId(), "", session);
    }

    private void sendChatHistory(String history, Session session, String roomName) throws IOException, EncodeException {
        if (history != null && !(history.isBlank())) {
            // split the history into messages
            String arr[] = history.split("~S~");
            for (String message : arr) {
                // send the message to the user
                session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
            }
            // add the room to the history list
            roomHistoryList.put(roomName, history + " \\n " + roomName + " room resumed.~S~");
        }
    }

    private void leaveRoom(Session session) throws IOException, EncodeException {
        // get the user id
        String userId = session.getId();
        // find the chat room by the user id
        ChatRoom chatRoom = findChatRoomByUserId(userId);

        if (chatRoom != null) {
            // Get the room name
            String roomName = chatRoom.getRoomName();
            //   get the username
            String username = chatRoom.getUsers().get(userId);
            // remove the user from the chat room
            chatRoom.removeUser(userId);

            if (chatRoom.isEmpty()) {
                // save the chat room history
                saveChatRoomHistory(roomName, roomHistoryList.get(roomName));
                // remove the room from the active rooms list
                activeChatRooms.remove(roomName);
                // Remove the chat room from the map
                chatRooms.remove(chatRoom.getRoomID());
            }
// update the room history
            updateRoomHistory(roomName, username + " left the chat room.~S~");
            // broadcast the goodbye message
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
        // get the room history
        String logHistory = roomHistoryList.get(roomName);
        // update the room history
        roomHistoryList.put(roomName, logHistory + "\\n" + message);
    }

    private void broadcastMessageToPeersInRoom(ChatRoom chatRoom, Session session, String message) throws IOException, EncodeException {
        for (Session peer : session.getOpenSessions()) {
            if (chatRoom.inRoom(peer.getId())) {
                // send the message to the user
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
            }
        }
    }

    private void welcomeBroadcast(ChatRoom chatRoom, Session session, String message) throws IOException, EncodeException {
        for (Session peer : session.getOpenSessions()) {
            if (chatRoom.inRoom(peer.getId())) {
                // send the message to the user
                System.out.println("Sending welcome message to " + peer.getId());
                // send the message to the user
                peer.getBasicRemote().sendText("{\"type\": \"userJoin\", \"message\":\"" + message + "\"}");
                if(!peer.getId().equals(session.getId())) {
                    // send the message to the user
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
                }
            }
        }
    }

    private void goodbyeBroadcast(ChatRoom chatRoom, Session session, String message) throws IOException, EncodeException {
        for (Session peer : session.getOpenSessions()) {
            if (chatRoom.inRoom(peer.getId())) {
                // send the message to the user
                peer.getBasicRemote().sendText("{\"type\": \"userLeave\", \"message\":\"" + message + "\"}");
                // send the message to the user
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"" + message + "\"}");
            }
        }
    }

    private void handleUserMessage(String comm, Session session) throws IOException, EncodeException {
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

        String userID = session.getId();
        // find the chat room by the user id
        ChatRoom chatRoom = findChatRoomByUserId(userID);

        if (chatRoom == null) {
            return;
        }

        if (chatRoom.getUsers().get(userID) != null && !chatRoom.getUsers().get(userID).isEmpty()) {
            // handle the chat message
            handleChatMessage(chatRoom, session, userID, message);
        } else {
            // handle the username message
            handleUsernameMessage(chatRoom, session, userID, message);

        }
// get the room name
        String roomName = chatRoom.getRoomName();
        // save the chat room history
        saveChatRoomHistory(roomName, roomHistoryList.get(roomName));
    }

    private void handleChatMessage(ChatRoom chatRoom, Session session, String userID, String message) throws IOException, EncodeException {
        String roomName = chatRoom.getRoomName();
        String username = chatRoom.getUsers().get(userID);
// update the room history
        updateRoomHistory(roomName, "(" + username + "): " + message + "~S~");
        // broadcast the message
        broadcastMessageToPeersInRoom(chatRoom, session, "(" + username + "): " + message);
    }

    private void handleUsernameMessage(ChatRoom chatRoom, Session session, String userID, String message) throws IOException, EncodeException {
        // set the username
        chatRoom.setUserName(userID, message, session);

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome, " + message + "!\"}");
        // get the room name
        String roomName = chatRoom.getRoomName();
        // update the room history
        updateRoomHistory(roomName, message + " joined the chat room.~S~");
        // broadcast the welcome message
        welcomeBroadcast(chatRoom, session, "(Server): " + message + " joined the chat room.");
    }

    //Function to get the usernames of the users in a chatroom
    public static List<String> getUsernames(String roomName) {
        // Create a list to store the usernames
        List<String> usernames = new ArrayList<>();
        // Create a chatroom to store the target chatroom
        ChatRoom targetChatRoom = null;
        for (ChatRoom chatRoom : chatRooms.values()) {
            if (chatRoom.getRoomName().equals(roomName)) {
                // Set the target chatroom to the chatroom with the same name as the roomname
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
