    // Create the chart outside the fetch function
    var ctx = document.getElementById('myChart').getContext('2d');
    var chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Sensor Value',
                backgroundColor: 'rgb(255, 99, 132)',
                borderColor: 'rgb(255, 99, 132)',
                data: []
            }]
        },
        options: {}
    });
    
    // Function to fetch new data and update the chart
    function updateChart() {
        fetch('/api/sensor-data')
            .then(response => response.json())
            .then(data => {
                // Update the chart with the new data
                chart.data.labels = data.map(item => new Date(item.timestamp).toLocaleDateString());
                chart.data.datasets[0].data = data.map(item => item.sensorValue);
                chart.update();
            })
            .catch(error => console.error('Error:', error));
    }
    
    // Update the chart immediately and then every 5 seconds
    updateChart();
    setInterval(updateChart, 5000);