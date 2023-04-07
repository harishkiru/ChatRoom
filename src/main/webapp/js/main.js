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

document.addEventListener('DOMContentLoaded', () => {
    const activeChatRoomsContainer = document.getElementById('activeChatRooms');
    const apiEndpoint = 'http://localhost:8080/WSChatServer-1.0-SNAPSHOT/api/endpoints/activeRooms';

    fetchActiveChatRooms();

    async function fetchActiveChatRooms() {
        try {
            const response = await fetch(apiEndpoint);
            const data = await response.text();
            //Remove first and last character (square brackets)
            const chatRooms = data.substring(1, data.length - 1).split(',').map(room => room.trim());
            displayActiveChatRooms(chatRooms);
        } catch (error) {
            console.error('Error fetching active chat rooms:', error);
        }
    }


    function displayActiveChatRooms(chatRooms) {
        const ul = document.createElement('ul');
        chatRooms.forEach((room) => {
            const li = document.createElement('li');
            li.textContent = room;
            ul.appendChild(li);
        });
        activeChatRoomsContainer.appendChild(ul);
    }
});



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


