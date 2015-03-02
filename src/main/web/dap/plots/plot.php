<?php
header('Access-Control-Allow-Origin: *');
$bins = isset($_REQUEST["bins"]) ? $_REQUEST["bins"] : "minutes";
$time_unit = 60*1000;							//Milli-seconds in a minute
$time_window = 24*60*60;					//Seconds in one day

if($bins == "hours"){
	$time_unit = 60*60*1000;
}else if($bins == "days"){
	$time_unit = 24*60*60*1000;
	$time_window = 7*24*60*60;			//Seconds in one week
}else if($bins != "minutes"){   	//Make sure a bins is one of these 3 values, default to minutes
  $bins = "minutes";
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
	$start_time = $document["start_time"];
	$delta_time = time() - round($start_time/1000);

	if($delta_time < $time_window) {
		$timestamp = round($start_time / $time_unit);
		//echo $timestamp . "<br>\n";

		$tasks_per_x[$timestamp] = isset($tasks_per_x[$timestamp]) ? $tasks_per_x[$timestamp]+1 : 1;

		if(!$document["success"]){
			$failures_per_x[$timestamp] = isset($failures_per_x[$timestamp]) ? $failures_per_x[$timestamp]+1 : 1;
		}
	}
}

if(time() - filemtime("tmp/$bins.png") > 60) {    //Don't update plot more than once each minute
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
}

//Print out overall mean performance
echo array_sum(array_values($tasks_per_x))/count($tasks_per_x) . " ";

if(empty($failures_per_x)){
	echo 0;
}else{
	echo array_sum(array_values($failures_per_x))/count($failures_per_x);
}
?>
