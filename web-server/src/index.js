const mongoose = require('mongoose');
const path = require('path');
var express = require('express');
var fs = require('fs');
var url = require('url');
var http = require('http');
var WebSocket = require('ws');
var app = express();
const Sensor = require('./model/sensor');
const LedTime = require('./model/LedTime');
const DoorStatus = require('./model/DoorStatus');
const db = require('./config/db/index.db');
app.use(express.static(path.join(__dirname, 'public')));

db.connect();
app.get('/api/doorStatuses', (req, res) => {
    DoorStatus.find({})
        .sort({createdAt: -1}) // Sort by createdAt in descending order
        .limit(30) // Limit to 30 items
        .then(data => {
            // Map the data to the desired format
            let formattedData = data.map(item => {
                let status;
                if (item.doorStatus === 'DOOR_OPEN' ) {
                    status = 'Success';
                } else if (item.doorStatus === 'DOOR_OPEN_FAIL') {
                    status = 'Fail';
                }else if (item.doorStatus === 'DOOR_CLOSE') {
                    return null
                }

                return {
                    status: status,
                    time: item.createdAt
                };
            }).filter(item => item !== null);

            res.json(formattedData);
        }).catch(err => {
            res.status(500).send(err);
        });
});

app.get('/api/led-operation-time', function(req, res) {
    LedTime.aggregate([
      {
        $group: {
          _id: {
            ledType: "$ledType",
            year: { $year: "$ledOnTime" },
            month: { $month: "$ledOnTime" },
            day: { $dayOfMonth: "$ledOnTime" },
          },
          totalOperationTime: { $sum: "$ledOperationTime" }
        }
      }
    ]).then((results) => {
      res.json(results);
    }).catch((err) => {
      res.status(500).send(err);
    });
  });
  app.get('/chart1', function(req, res) {
    res.sendFile(__dirname + '/public/chart1.html');})
app.get('/chart', function(req, res) {
    res.sendFile(__dirname + '/public/chart.html');})
app.get('/api/sensor-data', function(req, res) {
    // Query the database for the latest 10 sensor data
    Sensor.find({})
        .sort({createdAt: -1}) // Sort by createdAt in descending order
        .limit(10) // Limit to 10 items
        .then(data => {
            res.json(data);
        }).catch(err => {
            res.status(500).send(err);
        });
});
async function getLastLedOnTime(ledType) {
    // Truy vấn cơ sở dữ liệu để lấy thời gian bật đèn gần nhất
    const lastLedOnTime = await LedTime.findOne({ ledType: ledType })
        .sort({ ledOnTime: -1 }) // Sắp xếp theo ledOnTime giảm dần
        .select('ledOnTime') // Chỉ lấy trường ledOnTime
        .lean(); // Chuyển đổi kết quả truy vấn thành một đối tượng JavaScript thuần

    // Chuyển đổi kết quả truy vấn thành một đối tượng Date
    return new Date(lastLedOnTime.ledOnTime);
}

// app.get('/1', function(req, res) {
//     res.sendFile(__dirname + '/public/index1.html');})

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
    var ledPKOnTime, ledPKOffTime; 
    var ledPNOnTime, ledPNOffTime;
    socket.on('message', async function(message) {
        // Parse the incoming message as JSON
        const data = JSON.parse(message);
        // console.log(data);
        if(data.hasOwnProperty("doorStatus")){
            const doorStatus = new DoorStatus(data);
            doorStatus.save().then(() => {
                // console.log('Sensor data saved to database');
            }).catch((err) => {
                // console.error('Error saving sensor data to database:', err);
            });
        }
        if (data.hasOwnProperty("sensorValue")) {
            const sensor = new Sensor(data);
            sensor.save().then(() => {
                // console.log('Sensor data saved to database');
            }).catch((err) => {
                // console.error('Error saving sensor data to database:', err);
            });
        }if (data.hasOwnProperty("ledStatus")) {
            switch (data.ledStatus) {
                case "LED_ON_PK":
                    ledPKOnTime = new Date(); // Store the current time when led_pk is turned on
                    console.log('LED_PK turned on at ' + ledPKOnTime);
                    break;
                case "LED_OFF_PK":
                    ledPKOffTime = new Date(); // Store the current time when led_pk is turned off
                    console.log('LED_PK turned off at ' + ledPKOffTime);
                    if (isNaN(ledPKOnTime)) {
                        ledPKOnTime = await getLastLedOnTime("LED_PK");
                    }
                    // Calculate the operation time of led_pk and save it to the database
                    saveLEDOperationTime(ledPKOnTime, ledPKOffTime, "LED_PK");
                    break;
                case "LED_ON_PN":
                    ledPNOnTime = new Date(); // Store the current time when led_pn is turned on
                    console.log('LED_PN turned on at ' + ledPNOnTime);
                    break;
                case "LED_OFF_PN":
                    ledPNOffTime = new Date(); // Store the current time when led_pn is turned off
                    console.log('LED_PN turned off at ' + ledPNOffTime);
                    if (isNaN(ledPNOnTime)) {
                        ledPNOnTime = await getLastLedOnTime("LED_PN");
                    }
                    // Calculate the operation time of led_pn and save it to the database
                    saveLEDOperationTime(ledPNOnTime, ledPNOffTime, "LED_PN");
                    break;
            }
        }
        
        console.log('received: %s', message);
        broadcast(socket, message);
    });
    socket.on('close', function() {
        var index = clients.indexOf(socket);
        clients.splice(index, 1);
        console.log('disconnected');
        if (ledPKOnTime && !ledPKOffTime) {
            // Create a new LEDTime object and save it to the database
            const ledTime = new LedTime({
                ledOnTime: ledPKOnTime,
                ledOffTime: 0, // Set off time to 0
                ledOperationTime: 0, // Set operation time to 0
                ledType: "LED_PK"
            });
    
            ledTime.save().then(() => {
                console.log('LED operation time saved to database');
            }).catch((err) => {
                console.error('Error saving LED operation time to database:', err);
            });
        }
        if (ledPNOnTime && !ledPNOffTime) {
            // Create a new LEDTime object and save it to the database
            const ledTime = new LedTime({
                ledOnTime: ledPNOnTime,
                ledOffTime: 0, // Set off time to 0
                ledOperationTime: 0, // Set operation time to 0
                ledType: "LED_PN"
            });
    
            ledTime.save().then(() => {
                console.log('LED operation time saved to database');
            }).catch((err) => {
                console.error('Error saving LED operation time to database:', err);
            });
        }
    });
});
function saveLEDOperationTime(ledOnTime, ledOffTime, ledType) {
    // Calculate the operation time of the LED
    var ledOperationTime = ledOffTime - ledOnTime;
    console.log(ledType + ' was on for ' + ledOperationTime + ' milliseconds');

    // Create a new LEDTime object and save it to the database
    const ledTime = new LedTime({
      ledOnTime: ledOnTime,
      ledOffTime: ledOffTime,
      ledOperationTime: ledOperationTime,
      ledType: ledType // Add a new field to store the type of the LED
    });

    ledTime.save().then(() => {
      console.log('LED operation time saved to database');
    }).catch((err) => {
      console.error('Error saving LED operation time to database:', err);
    });
}
server.listen(3000);
console.log('Server listening on port 3000');