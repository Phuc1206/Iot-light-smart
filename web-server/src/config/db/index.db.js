const mongoose = require('mongoose');
async function connect(){
    try {
        await mongoose.connect('mongodb+srv://n20dcpt085:123@iot-tester.4ftfjse.mongodb.net/');
        console.log('Connected successfully!!!');
    } catch (error) {
        console.log('Error connecting');
    }
}
module.exports = {connect};