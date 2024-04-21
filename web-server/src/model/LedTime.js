const mongoose = require('mongoose');

const LedTimeSchema = new mongoose.Schema({
  ledOnTime: {type: Date},
  ledOffTime: { type: Date },
  ledOperationTime: { type: Number},
  ledType: { type: String}
});

module.exports = mongoose.model('LedTime', LedTimeSchema);