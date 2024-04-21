const mongoose = require('mongoose');

const Light_PNSchema = new mongoose.Schema({
  ledOnTime: {type: Date},
  ledOffTime: { type: Date },
  ledOperationTime: { type: Number}
});

module.exports = mongoose.model('Light_PN', Light_PNSchema);