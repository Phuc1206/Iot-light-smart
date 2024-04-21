const mongoose = require('mongoose');

const sensorValueSchema = new mongoose.Schema({
  sensorValue: {type: Number, require},
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('SensorValue', sensorValueSchema);