let ws;

async function fetchActiveChatRooms(apiEndpoint, activeChatRoomsContainer) {
    try {
        const response = await fetch(apiEndpoint);
        const data = await response.text();
        //Remove first and last character (square brackets)
        const chatRooms = data.substring(1, data.length - 1).split(',').map(room => room.trim());
        displayActiveChatRooms(chatRooms, activeChatRoomsContainer);
    } catch (error) {
        console.error('Error fetching active chat rooms:', error);
    }
}


function displayActiveChatRooms(chatRooms, activeChatRoomsContainer) {
    if(chatRooms[0] === "") {
        activeChatRoomsContainer.textContent = 'No active chat rooms.';
    } else {
        activeChatRoomsContainer.textContent = '';
        const ul = document.createElement('ul');
        chatRooms.forEach((room) => {
            const li = document.createElement('li');
            li.textContent = room;
            ul.appendChild(li);
        });
        activeChatRoomsContainer.appendChild(ul);
    }
}

function enterRoom() {
    console.log("?????");
    let code = document.getElementById("code").value;
    console.log(code);

    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/"+code);
    //Only open the chat window if the connection is successful

    ws.onmessage = function (event) {
        console.log(event.data);
        let message = JSON.parse(event.data);
        document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
    }

    ws.onopen = function () {
        console.log("WebSocket connection established");

        // Delay fetching active chat rooms by 1 second
        setTimeout(() => {
            const activeChatRoomsContainer = document.getElementById('activeChatRooms');
            const apiEndpoint = 'http://localhost:8080/WSChatServer-1.0-SNAPSHOT/api/endpoints/activeRooms';
            fetchActiveChatRooms(apiEndpoint, activeChatRoomsContainer);
        }, 60);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const activeChatRoomsContainer = document.getElementById('activeChatRooms');
    const apiEndpoint = 'http://localhost:8080/WSChatServer-1.0-SNAPSHOT/api/endpoints/activeRooms';

    fetchActiveChatRooms(apiEndpoint, activeChatRoomsContainer);


});


const chatLog = document.getElementById("log");
document.getElementById("input").addEventListener("keyup", function (event) {
    if (event.keyCode === 13) {
        let request = {"type":"chat", "msg":event.target.value};
        ws.send(JSON.stringify(request));
        event.target.value = "";
        chatLog.scrollTop = chatLog.scrollHeight;

    }
});

function timestamp() {
    var d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}

window.onLoad = function getRooms() {
    fetch("http://localhost:8080/WSChatServer-1.0-SNAPSHOT/api/endpoints/activeRooms")
        .then(response => response.json())
        .then(data => {
            console.log(data);
            let rooms = document.getElementById("rooms");
            for (let i = 0; i < data.length; i++) {
                let room = document.createElement("div");
                room.innerHTML = data[i];
                rooms.appendChild(room);
            }
        })
        .catch(error => console.log(error));
}