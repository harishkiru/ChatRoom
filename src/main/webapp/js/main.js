let ws;
function enterRoom() {
    console.log("?????");
    let code = document.getElementById("code").value;
    console.log(code);
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/"+code);

    ws.onmessage = function (event) {
        console.log(event.data);
        let message = JSON.parse(event.data);
        document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
    }
}
document.getElementById("input").addEventListener("keyup", function (event) {
    if (event.keyCode === 13) {
        let request = {"type":"chat", "msg":event.target.value};
        ws.send(JSON.stringify(request));
        event.target.value = "";
    }
});

function timestamp() {
    var d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}


window.onload = function showRooms() {
    //Get all existing rooms and display them
    let request = {"type":"rooms"};
    ws.send(JSON.stringify(request));

    ws.onmessage = function (event) {
        console.log(event.data);
        let message = JSON.parse(event.data);
        document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";

        //Display all existing rooms
        let rooms = message.message;
        for (let i = 0; i < rooms.length; i++) {
            let room = rooms[i];
            let roomDiv = document.createElement("div");
            roomDiv.className = "room";
            roomDiv.innerHTML = room;
            document.getElementById("rooms").appendChild(roomDiv);
        }

        //Add click event to each room
        let roomDivs = document.getElementsByClassName("room");
        for (let i = 0; i < roomDivs.length; i++) {
            let roomDiv = roomDivs[i];
            roomDiv.addEventListener("click", function (event) {
                let request = {"type":"join", "room":event.target.innerHTML};
                ws.send(JSON.stringify(request));
            });
        }
    }
}