package com.example.webchatserver.utils.History;

import com.example.webchatserver.utils.FileReaderWriter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;



@Path("/history")
public class History {
    @GET
    @Path("/{roomID}")
    @Produces("application/json")
    /**
     * GET HTTP METHOD
     * This method will read the content of roomID.json file and send it back to requester
     * **/
    public Response getRoomHistory(@PathParam("roomID") String roomID) {
        /*
         TODO: read contents from the roomID.json file and return it
         loading the resource directory
        */
        String path = "/Users/harish/Documents/CSCI2020U/Assignment2/kirubaharan-ali-patel-csci2020u-assignment02/src/main/resources/chatHistory";
        String history = "";
        File mainDir = null;

        //loading the resource directory
        mainDir = new File(path);

        // loading the file content into history
        try {
            history = FileReaderWriter.readHistoryFile(mainDir, roomID+".json");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        //build the json data for the response
        JSONObject mapper = new JSONObject();
        mapper.put("room", roomID);
        if(history!=null){
            mapper.put("log", history);
        }else{
            mapper.put("ERROR KEKW HAHAH LOL", "Room not found");
        }

        // build the Response object sending json data in the entity
        Response myResp = Response.status(200) // success
                .header("Content-Type", "application/json")
                .entity(mapper.toString()) // adding the json data
                .build();
        return myResp;
    }

    @POST
    @Path("/{roomID}")
    @Consumes("application/json")
    @Produces("application/json")
    /**
     * POST HTTP METHOD
     * This method will receive the history log of a room and store it into a json file
     * **/
    public Response saveRoomHistory(@PathParam("roomID") String roomID, String content) {

        // parse the consumed json data
        System.out.println(content + "OOO OOO AAAH AAAH");
        JSONObject mapper = new JSONObject(content);
        Map<String,Object> result = mapper.toMap();
        String filename = (String) result.get("room");

        // loading the resource directory
        String file = "/Users/harish/Documents/CSCI2020U/Assignment2/kirubaharan-ali-patel-csci2020u-assignment02/src/main/resources/chatHistory";


        File data = null;
        System.out.println(file);
        data = new File(file);

        try {
            // saving the chat log history to the roomID.json file in the resources folder
            FileReaderWriter.saveNewFile(data, filename+".json", (String) result.get("log"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


        Response myResp = Response.status(200) // success
                .header("Content-Type", "application/json")
                .build();
        return myResp;
    }
}