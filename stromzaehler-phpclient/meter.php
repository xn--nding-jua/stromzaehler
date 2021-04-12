<!DOCTYPE html>

<!--
PHP-Client for SML_2_Ethernet-Server for Smart-Energy-Meters
(c) 2021 Dr.-Ing. Christian Nöding
https://www.pcdimmer.de

Please download chart.js from GitHub:
https://github.com/chartjs/Chart.js/releases/download/v3.1.0/chart.js-3.1.0.tgz

Please donwload jquery.js from here:
https://code.jquery.com/jquery-3.6.0.min.js
-->

<html>
<head>
  <title>Stromverbrauch</title>
  <meta http-equiv="refresh" content="50" />
  <style type="text/css">
    .box { margin:25px auto;}
    .value180 { line-height:30px; float:center; font-size:20pt; color:#FF0000; }
    .value280 { line-height:30px; float:center; font-size:20pt; color:#00AF00; }
    .power { line-height:30px; float:center; font-size:20pt; color:#000000; }
    .shadow { text-shadow: 0 0 5px #123; }

    #chart-container {
      width: 100%;
      height: auto;
    }
  </style>
  <script type="text/javascript" src="js/jquery.min.js"></script>
  <script type="text/javascript" src="js/chart.min.js"></script>
</head>
<body>

<font face="Calibri">
<b><center>

<?php
  // read the current values from server
  $address = '127.0.0.1'; // local server
  $port = 51534;

  $connectionok = true;
  $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
  if ($socket === false) {
    $connectionok = false;
  }else{
    $result = socket_connect($socket, $address, $port);
    if ($result === false) {
      $connectionok = false;
    }else{
      /*
      // requesting the current power
      $cmd = "C:POWER?\n";
      socket_write($socket, $cmd, strlen($cmd));
      $answer = socket_read($socket, 50, PHP_NORMAL_READ);
      $power = intval(substr($answer, 4, strlen($answer)));
      */

      // request AppData containing full-data via ByteBuffer
      $cmd = "C:DATA=1\n"; // 2+21 bytes // request only current values
      //$cmd = "C:DATA=1"; // 2+21+1344 bytes = contains last 7 days
      socket_write($socket, $cmd, strlen($cmd));

      // read length of data
      $ans_length = socket_read($socket, 4, PHP_BINARY_READ); // read 4 bytes = INT
      $ans_length = unpack("N",$ans_length)[1];

      while (($ans_length<23) || ($ans_length>1000000)){
        $ans_length = socket_read($socket, 4, PHP_BINARY_READ); // read 4 bytes = INT
        $ans_length = unpack("N",$ans_length)[1];
      }

      // read chunksize
      $ans_chunksize = socket_read($socket, 4, PHP_BINARY_READ); // read 4 bytes = INT
      $ans_chunksize = unpack("N",$ans_chunksize)[1];

      // now read data - we are only expecting a single chunk, so for now we do not handle the chunksize
      $ans_gzdata = socket_read($socket, $ans_length, PHP_BINARY_READ); // read 4 bytes = INT

      // decompress gzip-data
      $ans_data = gzdecode($ans_gzdata);

      // get individual values
      $AppData_version = unpack("s", $ans_data, 0)[1];
      $AppData_IntegerArray = unpack("N5", $ans_data, 2); // unpack integer-array with 5 entries

      $AppData_value_180 = $AppData_IntegerArray[1];
      $AppData_value_280 = $AppData_IntegerArray[2];
      $AppData_value_180_day = $AppData_IntegerArray[3];
      $AppData_value_280_day = $AppData_IntegerArray[4];
      $AppData_power = $AppData_IntegerArray[5];

      $AppData_historylevel = unpack("c", $ans_data, 22)[1];

      if (($AppData_historylevel===1) || ($AppData_historylevel===2)) {
        // we received a Level 1 history. So we can decode this
        // read 168 4-byte ints for 180-history
        $AppData_180HistoryArray = unpack("N168", $ans_data, 23); // unpack integer-array with 168 entries
        // Wh -> kWh
        for ($i = 1; $i <= 83; $i++) {
          $AppData_180HistoryArray[$i] = $AppData_180HistoryArray[$i]/1000;
        }

        // read 168 4-byte ints for 280-history
        $AppData_280HistoryArray = unpack("N168", $ans_data, 23+672); // unpack integer-array with 168 entries
        // Wh -> kWh and put to negative part
        for ($i = 1; $i <= 83; $i++) {
          $AppData_280HistoryArray[$i] = -$AppData_280HistoryArray[$i]/1000;
        }

        // $AppData_180HistoryArray[1] contains energy of current hour, $AppData_180HistoryArray[2] the value of last hour, and so on
        //echo "180-values:" . $AppData_180HistoryArray[1] . " " . $AppData_180HistoryArray[2] . " " . $AppData_180HistoryArray[3] . "<br>";
        //echo "280-values:" . $AppData_280HistoryArray[1] . " " . $AppData_280HistoryArray[2] . " " . $AppData_280HistoryArray[3] . "<br>";

        if ($AppData_historylevel===2) {
          // we received level-2-hisotry and we can read 604800 4-byte power-values
          // TODO: read these values
        }
      }

      if ($AppData_historylevel===3) {
        // we received level-3-history and can read full history
        $AppData_level3history_bytes = unpack("c", $ans_data, 22)[1];
        $AppData_level3history_elements = unpack("N", $ans_data, 23)[1];
        // we are receiving x elements
        // TODO: unpack history-data
      }

      // close the socket-connection
      socket_close($socket);
    }
  }

  if ($connectionok===false) {
    $AppData_value_180 = -1;
    $AppData_value_280 = -1;
    $AppData_value_180_day = -1;
    $AppData_value_280_day = -1;
    $AppData_power = -1;
  }

  // plot the values on the website
  echo "<div class=\"box\">";
  echo "<div class=\"value180\">"."1.8.0 (Bezug) Gesamt: ". number_format($AppData_value_180/1000, 3, ',', '.') . " kWh | Heute: " . number_format($AppData_value_180_day/1000, 3, ',', '.') . " kWh</div>\n";
  echo "<div style=\"clear:both;\"></div>\n";
  echo "</div>\n";

  echo "<div class=\"box\">";
  echo "<div class=\"value280\">"."2.8.0 (Einspeisung) Gesamt: ". number_format($AppData_value_280/1000, 3, ',', '.') . " kWh | Heute: " . number_format($AppData_value_280_day/1000, 3, ',', '.') . " kWh</div>\n";
  echo "<div style=\"clear:both;\"></div>\n";
  echo "</div>\n";

  echo "<div class=\"box\">";
  echo "<div class=\"power\">"."Aktuelle Leistung: ". number_format($AppData_power, 0, ',', '.') . " W</div>\n";
  echo "<div style=\"clear:both;\"></div>\n";
  echo "</div>\n";

?>

</center></b>
</font>

<!-- Draw the chart-graph for the 7-day-history -->
<div id="chart-container">
    <canvas id="graphCanvas" width="400" height="200"></canvas>
</div>

<script>

const data = {
  datasets: [
    {
      label: '1.8.0',
      backgroundColor: 'rgb(255, 0, 0)',
      borderColor: 'rgb(255, 0, 0)',
      data: <?php echo json_encode($AppData_180HistoryArray); ?>
    },
    {
      label: '2.8.0',
      backgroundColor: 'rgb(0, 192, 0)',
      borderColor: 'rgb(0, 192, 0)',
      data: <?php echo json_encode($AppData_280HistoryArray); ?>
    }
  ]
};

const config = {
  type: 'bar',
  data,
  options: {
    scales: {
      x: {
        reverse: true,
        stacked: true
      }
    },
    responsive: true,
    plugins: {
      legend: {
        position: 'top'
      },
      title: {
        display: true,
        text: '7-Tage Smart-Meter-History'
      }
    }
  }
};

  var graphCanvas = new Chart(document.getElementById('graphCanvas'), config);
</script>

</body>
</html>
