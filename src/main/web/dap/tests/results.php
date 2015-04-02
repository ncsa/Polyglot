<html>
  <head><title>TEST Results</title></head>
  <body>
    <table border=1>
      <tr>
        <th>Host</th>
        <th>Type</th>
        <th>Date</th>
        <th>Time</th>
        <th>Elapsed</th>
        <th>Total</th>
        <th>Success</th>
        <th>Failures</th>
      </tr>
<?php
$m = new MongoClient("mongo.ncsa.illinois.edu");
$db = $m->{"browndog"};
$collection = $db->{"tests"};
$cursor = $collection->find()->sort(array("date" => -1))->limit(50);
foreach ($cursor as $doc) {
  if ($doc['failures'] != 0) {
    print("      <tr style=\"background: #FFBBBB; color: black;\">\n");
  } else {
    print("      <tr>\n");
  }
  print("        <td>${doc['hostname']}</td>\n");
  print("        <td>${doc['type']}</td>\n");
  print("        <td>" . date('Y-M-d', $doc['date']->sec) . "</td>\n");
  print("        <td>" . date('h:i:s', $doc['date']->sec) . "</td>\n");
  print("        <td>${doc['elapsed_time']}</td>\n");
  print("        <td>${doc['total']}</td>\n");
  print("        <td>${doc['success']}</td>\n");
  print("        <td title=\"" . htmlentities($doc['message']) . "\">${doc['failures']}</td>\n");
  print("      </tr>\n");
}
?>
    </table>
  </body>
</html>
