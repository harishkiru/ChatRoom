package com.example.webchatserver;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * This is a class that has services
 * In our case, we are using this to generate unique room IDs**/
@WebServlet(name = "chatServlet", value = "/chat-servlet")
public class ChatServlet extends HttpServlet {
    private String message;

    //static so this set is unique
    // set of rooms

    public static Set<String> rooms = new HashSet<>();



    /**
     * Method generates unique room codes
     * **/
    public static String generatingRandomUpperAlphanumericString(int length) {
        // generating random string
        String generatedString = RandomStringUtils.randomAlphanumeric(length).toUpperCase();
        // generating unique room code
        while (rooms.contains(generatedString)){
            // generating random string
            generatedString = RandomStringUtils.randomAlphanumeric(length).toUpperCase();
        }
        // adding the room to the set
        rooms.add(generatedString);
// returning the room code
        return generatedString;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // setting the content type to plain text
        response.setContentType("text/plain");

        // send the random code as the response's content
        // creating a print writer
        PrintWriter out = response.getWriter();
        // printing the random code
        out.println(generatingRandomUpperAlphanumericString(5));

    }

    public void destroy() {
    }
}