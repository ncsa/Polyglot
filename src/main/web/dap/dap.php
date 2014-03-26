<?php
$servers = $_GET["servers"];
$software = $_GET["software"];
$inputs = $_GET["inputs"];
$outputs = $_GET["outputs"];
$requests = $_GET["requests"];
$headings = $_GET["headings"];

$m = new MongoClient();
$db = $m->dap;

//Servers
if($servers){
	$collection = $db->servers;
	$cursor = $collection->find();

	echo "<h2>Software Servers</h2>\n";

	foreach($cursor as $document) {
		echo $document["host"] . "<br>\n";
	}
}

//Software
if($software){
	$collection = $db->software;
	$cursor = $collection->find();

	echo "<h2>Software</h2>\n";

	foreach($cursor as $document) {
		echo $document["name"] . "<br>\n";
	}
}

//Inputs
if($inputs){
	$collection = $db->inputs;
	$cursor = $collection->find();
	$FIRST = true;

	echo "<h2>Supported Inputs</h2>\n";

	foreach($cursor as $document) {
		if($FIRST) {
			$FIRST = false;
		} else {
			echo ", ";
		}

		echo $document["extension"];
	}
}

//Outputs
if($outputs){
	$collection = $db->outputs;
	$cursor = $collection->find();
	$FIRST = true;

	echo "<br>\n";
	echo "<h2>Supported Outputs</h2>\n";

	foreach($cursor as $document) {
		if($FIRST) {
			$FIRST = false;
		} else {
			echo ", ";
		}

		echo $document["extension"];
	}
}

//Requests
if($requests){
	$collection = $db->requests;
	$cursor = $collection->find();

	echo "<br>\n";
	echo "<h2>Requests</h2>\n";
	echo "<table border=\"1\">\n";
	echo "<tr><th>Address</th><th>Filename</th><th>Filesize</th><th>Input</th><th>Output</th><th>Start Time</th><th>End Time</th><th>Success</th></tr>\n";

	foreach($cursor as $document) {
		echo "<tr>";
		echo "<td>" . $document["address"] . "</td>";
		echo "<td>" . $document["filename"] . "</td>";
		echo "<td>" . $document["filesize"] . "</td>";
		echo "<td>" . $document["input"] . "</td>";
		echo "<td>" . $document["output"] . "</td>";
		echo "<td>" . $document["start_time"] . "</td>";
		echo "<td>" . $document["end_time"] . "</td>";
		echo "<td>" . $document["success"] . "</td>";
	}

	echo "</table>\n";
}
?>
