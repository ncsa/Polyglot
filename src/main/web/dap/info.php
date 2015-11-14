<?php
header('Access-Control-Allow-Origin: *');
$servers = isset($_REQUEST["servers"]) ? $_REQUEST["servers"] : "";
$software = isset($_REQUEST["software"]) ? $_REQUEST["software"] : "";
$inputs = isset($_REQUEST["inputs"]) ? $_REQUEST["inputs"] : "";
$outputs = isset($_REQUEST["outputs"]) ? $_REQUEST["outputs"] : "";
$requests = isset($_REQUEST["requests"]) ? $_REQUEST["requests"] : "";
$bytes = isset($_REQUEST["bytes"]) ? $_REQUEST["bytes"] : "";
$files = isset($_REQUEST["files"]) ? $_REQUEST["files"] : "";
$headings = isset($_REQUEST["headings"]) ? $_REQUEST["headings"] : "";

$m = new MongoClient();
$db = $m->dap;

//Servers
if($servers) {
	$collection = $db->servers;
	$cursor = $collection->find();

	if($headings) {
		echo "<h2>Software Servers</h2>\n";
	}

	foreach($cursor as $document) {
		echo $document["host"] . "<br>\n";
	}
}

//Software
if($software){
	$collection = $db->software;
	$cursor = $collection->find();

	if($headings) {
		echo "<h2>Software</h2>\n";
	}

	foreach($cursor as $document) {
		echo $document["name"] . "<br>\n";
	}
}

//Inputs
if($inputs){
	$collection = $db->inputs;
	$cursor = $collection->find();
	$FIRST = true;

	if($headings) {
		echo "<h2>Supported Inputs</h2>\n";
	}

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

	if($headings) {
		echo "<br>\n";
		echo "<h2>Supported Outputs</h2>\n";
	}

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

	if($headings) {
		echo "<br>\n";
		echo "<h2>Requests</h2>\n";
	}

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

//Bytes
if($bytes){
	$collection = $db->requests;
	$cursor = $collection->find();
	$sum = 0;

	if($headings) {
		echo "<br>\n";
		echo "<h2>Bytes</h2>\n";
	}

	foreach($cursor as $document) {
		$sum += $document["filesize"];
	}

	echo $sum;
}

//Files
if($files){
	$collection = $db->requests;
	$cursor = $collection->find();
	$count = 0;

	if($headings) {
		echo "<br>\n";
		echo "<h2>Files</h2>\n";
	}

	foreach($cursor as $document) {
		$count++;
	}

	echo $count;
}
?>
