var button = document.getElementById('btn');
var led_pk = document.getElementById('led-pk');
var led_pn = document.getElementById('led-pn');
var led_nn = document.getElementById('led-nn');

var ledPnOffTimeInput = document.getElementById('led-pn-off-time');
ledPnOffTimeInput.disabled = true;
var ledPnOffTime;
var ledPnOffTimeChecked = false;
led_nn.disabled = true;
var led_tc = document.getElementsByName('led-tc')[0];
var mode_led = document.getElementById('mode-led');
var led_tc_status = "AUTO"
mode_led.innerHTML = "AUTO"
console.log(ledPnOffTimeInput)
            var url = window.location.host; // hàm trả về url của trang hiện tại kèm theo port
            console.log(url)
            var sensorValueSpan = document.getElementById('sensor-value');
            var ws = new WebSocket('ws://' + url + '/ws'); // mở 1 websocket với port 8000
            console.log(ws)
            console.log('connecting...')
            
            ws.onopen = function() //khi websocket được mở thì hàm này sẽ được thưc hiện
            {
                // document.getElementById('status').innerHTML = 'Connected';
                //  //khi websocket được mở, mới cho phép
                // console.log('connected...')
            };
            ws.onmessage = function(evt) // sự kiện xảy ra khi client nhận dữ liệu từ server
            {
                console.log(evt.data)
                var data = JSON.parse(evt.data);
                if (data.hasOwnProperty("sensorValue")) {
                    sensorValueSpan.innerText = data.sensorValue; // Update sensor value
                }
                if (data.hasOwnProperty("ledStatus")) { // Kiểm tra xem trạng thái LED có được gửi từ server hay không
                    if (data.ledStatus == 'LED_OFF_PK') {
                        led_pk.checked = false; // Nếu trạng thái LED là 'LED_OFF', tắt đèn LED
                    } else if (data.ledStatus == 'LED_ON_PK') {
                        led_pk.checked = true; // Nếu trạng thái LED là 'LED_ON', bật đèn LED
                    }
                    if (data.ledStatus == 'LED_OFF_PN') {
                        led_pn.checked = false; 
                    } else if (data.ledStatus == 'LED_ON_PN') {
                        led_pn.checked = true;
                    }
                    if (data.ledStatus == 'LED_OFF_NN') {
                        led_nn.checked = false; 
                    } else if (data.ledStatus == 'LED_ON_NN') {
                        led_nn.checked = true; 
                    }if (data.ledStatus == 'MANUAL') {
                        led_nn.checked = false; 
                    } else if (data.ledStatus == 'AUTO') {
                        led_nn.checked = true; 
                    }
                }
            }
            ws.onclose = function() { // hàm này sẽ được thực hiện khi đóng websocket
                led_pk.disabled = true;
                led_pn.disabled = true;
                led_nn.disabled = true;
                led_tc.disabled = true;
                document.getElementById('status').innerHTML = 'Connected';
            };
            led_pk.onchange = function() { // thực hiện thay đổi bật/tắt led
                var led_pk_status = 'LED_OFF_PK';
                if (led_pk.checked) {
                    led_pk_status = 'LED_ON_PK';
                }
                ws.send(JSON.stringify({
                    ledStatus: led_pk_status
                }));
            }
            led_pn.onchange = function() {
                var led_pn_status = 'LED_OFF_PN';
                if (led_pn.checked) {
                    led_pn_status = 'LED_ON_PN';
                    ledPnOffTimeInput.disabled = false;
                }else{
                   ledPnOffTimeInput.disabled = true;
                }
                ws.send(JSON.stringify({
                    ledStatus: led_pn_status
                }));
            }
            led_nn.onchange = function() { 
                var led_nn_status = 'LED_OFF_NN';
                if (led_nn.checked) {
                    led_nn_status = 'LED_ON_NN';
                }
                ws.send(JSON.stringify({
                    ledStatus: led_nn_status
                }));
            }
            ledPnOffTimeInput.onchange = function() {
                ledPnOffTime = ledPnOffTimeInput.value;
                ledPnOffTimeChecked = false;
              }
              led_tc.onchange = ()=>{
                  if(led_tc.checked){
                      led_nn.disabled = false;
                      led_tc_status = "MANUAL"
                      mode_led.innerHTML = "MANUAL";
                  }else {
                      led_nn.disabled = true;
                      led_tc_status = "AUTO"
                      mode_led.innerHTML = "AUTO";
                  }
                  ws.send(JSON.stringify({
                      ledStatus: led_tc_status
                  }));
              }

          setInterval(function() {
              var currentTime = new Date();
              var offTime = new Date();
              offTime.setHours(ledPnOffTime.split(':')[0]);
              offTime.setMinutes(ledPnOffTime.split(':')[1]);

              if (!ledPnOffTimeChecked && currentTime >= offTime) {
                  ledPnOffTimeChecked = true;
                  ws.send(JSON.stringify({
                      ledStatus: 'LED_OFF_PN'
                  }));
                  ledPnOffTimeInput.value = '';
              }
          }, 1000);
led_nn.addEventListener('mouseover', function(){
    this.style.cursor = 'no-drop';
})