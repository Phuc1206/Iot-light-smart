const mongoose = require('mongoose');

const Light_PKSchema = new mongoose.Schema({
  ledOnTime: {type: Date},
  ledOffTime: { type: Date },
  ledOperationTime: { type: Number}
});

module.exports = mongoose.model('Light_PK', Light_PKSchema);