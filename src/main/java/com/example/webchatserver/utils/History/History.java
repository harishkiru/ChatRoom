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
        //main path (only works on windows) the other one is for harish who codes on mac
        File mainDirectory = new File(getClass().getClassLoader().getResource("chatHistory").getFile());
        String path = mainDirectory.getAbsolutePath();
        //String path = "/Users/harish/Documents/CSCI2020U/Assignment2/kirubaharan-ali-patel-csci2020u-assignment02/src/main/resources/chatHistory";
        String history = "";
        File mainDir = null;

        //loading the resource directory
        mainDir = new File(path);

        // loading the file content into history
        try {
            // read the file
            history = FileReaderWriter.readHistoryFile(mainDir, roomID+".json");
        } catch (FileNotFoundException e) {
            // if file not found, throw an exception
            throw new RuntimeException(e);
        }

        //build the json data for the response
        // create a json object
        JSONObject mapper = new JSONObject();
        // add the roomID to the json object
        mapper.put("room", roomID);
        if(history!=null){
            // add the history to the json object
            mapper.put("log", history);
        }else{
            // error if room not found
            mapper.put("ERROR KEKW HAHAH LOL", "Room not found");
        }

        // build the Response object sending json data in the entity
        // success
        Response myResp = Response.status(200)
                .header("Content-Type", "application/json")
                // adding the json data
                .entity(mapper.toString())
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
        //System.out.println(content + "OOO OOO AAAH AAAH");
        JSONObject mapper = new JSONObject(content);
        // convert the json data to a map
        Map<String,Object> result = mapper.toMap();
        // get the roomID from the map
        String filename = (String) result.get("room");

        // loading the resource directory
        //String file = "/Users/harish/Documents/CSCI2020U/Assignment2/kirubaharan-ali-patel-csci2020u-assignment02/src/main/resources/chatHistory";
        //main path (only works on windows) the other one is for harish who codes on mac
        File mainDirectory = new File(getClass().getClassLoader().getResource("chatHistory").getFile());
        String file = mainDirectory.getAbsolutePath();



        File data = null;
        // create a file object
        data = new File(file);


        try {
            // saving the chat log history to the roomID.json file in the resources folder
            //get("log")
            FileReaderWriter.saveNewFile(data, filename+".json", (String) result.get("log"));
        } catch (FileNotFoundException e) {
            // if file not found, throw an exception
            throw new RuntimeException(e);
        }


        Response myResp = Response.status(200) // success
                .header("Content-Type", "application/json")
                .build();
        return myResp;
    }
}