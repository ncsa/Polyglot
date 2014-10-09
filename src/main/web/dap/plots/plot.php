<?php
$bins = isset($_REQUEST["bins"]) ? $_REQUEST["bins"] : "minutes";
$time_unit = 60*1000;				//Milli-seconds in a minute
$time_window = 24*60*60;		//Seconds in one day

if($bins == "hours"){
	$time_unit = 60*60*1000;
}else if($bins == "days"){
	$time_unit = 24*60*60*1000;
	$time_window = 7*24*60*60;
}

$tasks_per_x = array();
$failures_per_x = array();

//Connect to mongo
$m = new MongoClient();
$db = $m->{"dap"};
$collection = $db->{"requests"};

//Bin the start times of the tasks
$cursor = $collection->find();

foreach($cursor as $document) {
	//print_r($document);
	$timestamp = round($document["start_time"] / $time_unit);
	//echo $timestamp . "<br>\n";

	if(!array_key_exists($timestamp, $tasks_per_x)) $tasks_per_x[$timestamp] = 0;	//ToDo: Why is this always true now?
	$tasks_per_x[$timestamp]++;

	if(!$document["success"]){
		if(!array_key_exists($timestamp, $failures_per_x)) $failures_per_x[$timestamp] = 0;	//ToDo: Why is this always true now?
		$failures_per_x[$timestamp]++;
	}
}

//Save the resulting histogram to a text file
$keys = array_keys($tasks_per_x);
$fp = fopen("tmp/$bins.txt", "w+");

foreach($keys as $key) {
	$failures = 0;

	if(array_key_exists($key, $failures_per_x) && $failures_per_x[$key]){
		$failures = $failures_per_x[$key];
	}

	if($time_unit <= 3600000){		//If less than or equal to an hour
		$point = date('Y-m-d H:i', $key*$time_unit/1000) . " " . $key . " " . $tasks_per_x[$key] . " " . $failures;
	}else{
		$point = date('Y-m-d m-d', $key*$time_unit/1000) . " " . $key . " " . $tasks_per_x[$key] . " " . $failures;
	}

	fwrite($fp, "$point\n");

	//echo "$point<br>\n";
}

fclose($fp);

//Call GNUPlot to generate a plot
exec("gnuplot $bins.gnuplot");

//Print out overall mean performance
echo array_sum(array_values($tasks_per_x))/count($tasks_per_x) . " ";

if(empty($failures_per_x)){
	echo 0;
}else{
	echo array_sum(array_values($failures_per_x))/count($failures_per_x);
}
?>
