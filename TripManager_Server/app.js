// Node.js basic web socket.io communication example
// load extend module 
var express = require('express');
var http = require('http');
var socketIO = require('socket.io');

// load custom module
var Event = require('./event');
var Log = require('./io');

// To use MySql database
var mysql = require('mysql');

// create connection instance to connect database 
var connection = mysql.createConnection({
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'tripmanager'
});

connection.connect(function (err) {
    if (err)
        Log.put('MYSQL', 'Connection error : ' + err.message);
    else
        Log.put('MYSQL', 'Connection success');
})

// To use FCM
var admin = require("firebase-admin");
var serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://tripmanager-178913.firebaseio.com"
});

var app = express();
var server = http.createServer(app);
// server upgrade to socket.io 
var io = socketIO(server);

app.get('/', function (request, response) {
    // if client connect root(/), send first file
    // response.sendfile(__dirname + "/index.html");
    response.send("Welcome to server from web. this page is test page.");
});

// server start listening
server.listen(8080, function () {
    Log.put('SERVER', 'Server is listening on port 8080.');
});

var client_count = 0;

/* when socket.io server receive client, event occur
   callback parameter is socket that remain server and client connection */
io.on('connection', function (socket) {
	 client_count++;
    Log.put('SERVER', 'A client Conneced. - ' + client_count);

    // recevie event named 'call' from connected client
    socket.on('application', function (data) {
        var json = JSON.parse(data);
        switch (json.sub_event) {
            case 'version':
                Event.versionApplication(connection, admin, json, socket);
                break;
            case 'user':
                Event.userApplication(connection, admin, json, socket);
                break;
        }
    });

    socket.on('member', function (data) {
        var json = JSON.parse(data);

        switch (json.sub_event) {
            case 'check':
                Event.checkMember(connection, admin, json, socket);
                break;
            case 'register':
                Event.registerMember(connection, admin, json, socket);
                break;
            case 'update':
                Event.updateMember(connection, admin, json, socket);
                break;
            case 'unregister':
                Event.unregisterMember(connection, admin, json, socket);
                break;
        }
    });

    socket.on('group', function (data) {
        var json = JSON.parse(data);

        switch (json.sub_event) {
            case 'create':
                Event.createGroup(connection, admin, json, socket);
                break;
            case 'update':
                Event.updateGroup(connection, admin, json, socket);
                break;
            case 'find':
                Event.findGroup(connection, admin, json, socket);
                break;
            case 'join':
                Event.joinGroup(connection, admin, json, socket);
                break;
            case 'exit':
                Event.exitGroup(connection, admin, json, socket);
                break;
            case 'display':
                Event.displayGroup(connection, admin, json, socket);
                break;
            case 'getEditor':
                Event.getEditorGroup(connection, admin, json, socket);
                break;
        }
    });

    socket.on('schedule', function (data) {
        var json = JSON.parse(data);

        switch (json.sub_event) {
            case 'create':
                Event.createSchedule(connection, admin, json, socket);
                break;
            case 'update':
                Event.updateSchedule(connection, admin, json, socket);
                break;
            case 'delete':
                Event.deleteSchedule(connection, admin, json, socket);
                break;
            case 'display':
                Event.displaySchedule(connection, admin, json, socket);
                break;
        }
    });

    socket.on('notification', function (data) {
        var json = JSON.parse(data);

        switch (json.sub_event) {
            case 'create':
                Event.createNotification(connection, admin, json, socket);
                break;
            case 'update':
                Event.updateNotification(connection, admin, json, socket);
                break;
            case 'delete':
                Event.deleteNotification(connection, admin, json, socket);
                break;
            case 'display':
                Event.displayNotification(connection, admin, json, socket);
                break;
        }
    });

    socket.on('follower', function (data) {
        var json = JSON.parse(data);

        switch (json.sub_event) {
            case 'find':
                Event.findFollower(connection, admin, json, socket);
                break;
            case 'display':
                Event.displayFollower(connection, admin, json, socket);
                break;
            case 'trace':
                Event.traceFollower(connection, admin, json, socket);
                break;
            case 'getlatlng':
                Event.getLatlngFollower(connection, admin, json, socket);
                break;
            case 'alram':
                Event.alramFollower(connection, admin, json, socket);
                break;
            case 'location':
                Event.locationFollower(connection, admin, json, socket);
                break;
            case 'accessible':
                Event.accessibleFollower(connection, admin, json, socket);
                break;
            case 'gpsboost':
                Event.GPSBoostFollower(connection, admin, json, socket);
                break;
        }
    });

    socket.on('disconnect', function () {
		  client_count--;
        Log.put('SERVER', 'A client disconnected.');
    });
});

/*
var testing = {
    data: {
        title: 'Hello FCM !',
        body: 'Bye FCM !!'
    }
};

admin.messaging().sendToDevice(Token, Message)
    .then(function (response) {
        console.log('Successfully sent message:', response);
    })
    .catch(function (error) {
        console.log('Error sending message:', error);
    });
*/
