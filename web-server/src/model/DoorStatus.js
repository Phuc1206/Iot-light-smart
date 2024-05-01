const mongoose = require('mongoose');


const doorStatusSchema = new mongoose.Schema({
  doorStatus: {type: String, require},
  
},{
    timestamps:true,
});

module.exports = mongoose.model('DoorStatus', doorStatusSchema);