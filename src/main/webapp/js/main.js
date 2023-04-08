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

async function enterRoom() {
    let code = document.getElementById("code").value;
    console.log(code);

    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/" + code);

    ws.onmessage = async function (event) {
        let message = JSON.parse(event.data);
        console.log("Message received: " + message.type);

        if (message.type === "userJoin") {
            updateActiveUsersList(code);
        }  else if (message.type === "roomEmpty") {
            // Handle roomEmpty event by updating the list of active chat rooms
            await fetchActiveChatRooms(apiEndpoint, activeChatRoomsContainer);
        } else {
            document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
        }
    };

    ws.onopen = async function () {
        console.log("WebSocket connection established");
        console.log(code);

        updateActiveUsersList(code);
    };
}

async function updateActiveUsersList(code) {
    try {
        const response = await fetch("http://localhost:8080/WSChatServer-1.0-SNAPSHOT/api/endpoints/activeUsers/" + code);
        const data = await response.text();
        const activeUsers = data.substring(1, data.length - 1).split(',').map(user => user.trim());
        const filteredUsers = activeUsers.filter(user => user.length < 30);
        const activeUsersContainer = document.querySelector('.small-rectangular-box');
        activeUsersContainer.innerHTML = '';

        filteredUsers.forEach(user => {
            addUserToActiveUsersList(user);
        });

    } catch (error) {
        console.error('Error fetching active users:', error);
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
            //console.log(data);
            let rooms = document.getElementById("rooms");
            for (let i = 0; i < data.length; i++) {
                let room = document.createElement("div");
                room.innerHTML = data[i];
                rooms.appendChild(room);
            }
        })
        .catch(error => console.log(error));
}

function addUserToActiveUsersList(username) {
    setTimeout(async () => {
        console.log(username + " added");
        const activeUsersContainer = document.querySelector('.small-rectangular-box');
        const userElement = document.createElement('div');
        userElement.textContent = username;
        activeUsersContainer.appendChild(userElement);
    } , 500);
}
