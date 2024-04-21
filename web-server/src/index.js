const mongoose = require('mongoose');
var express = require('express');
var fs = require('fs');
var url = require('url');
var http = require('http');
var WebSocket = require('ws');
var app = express();


const db = require('./config/db');

// function gửi yêu cầu(response) từ phía server hoặc nhận yêu cầu (request) của client gửi lên
app.get('/', function(req, res) {
    res.sendFile(__dirname + '/public/index.html');})
// create http server
var server = http.createServer(app);
var ws = new WebSocket.Server({
    server
});
var clients = [];

function broadcast(socket, data) {
    console.log(clients.length);
    for (var i = 0; i < clients.length; i++) {
        if (clients[i] != socket) {
            clients[i].send(data);
        }
    }
}

ws.on('connection', function(socket, req) {
    clients.push(socket);
    socket.on('message', function(message) {
        console.log('received: %s', message);
        broadcast(socket, message);
    });
    socket.on('close', function() {
        var index = clients.indexOf(socket);
        clients.splice(index, 1);
        console.log('disconnected');
    });
});
server.listen(3000);
console.log('Server listening on port 3000');