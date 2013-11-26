<?php
/*
 * Test a task using our collection of sample files.  Useful for debugging the
 * Polyglot installation.
 */

$task = $_GET["task"];

//Read in samples
$samples = scandir("../misc/samples");
$files = array();

//Build extension->file map
for($i=0; $i<sizeof($samples); $i++){
	if($samples[$i] != "." && $samples[$i] != ".."){
		$arr = explode(".", $samples[$i]);
		$files[$arr[1]] = $samples[$i];
	}
}

//Submit the test job
$upload_path = "../uploads/";
$download_path = "../downloads/";
$timestamp = time();
$job = "Local_" . $timestamp;
$arr = explode(" ", $task);
$input_type = $arr[2];
$output_type = $arr[4];

mkdir("$upload_path$job");
copy("../misc/samples/$files[$input_type]", "$upload_path$job/$files[$input_type]");
file_put_contents("$upload_path$job/tasks", $task);
file_put_contents("$upload_path$job/commit", "");

//Wait for the job to be processed
while(!file_exists("$download_path$job/complete")){
  usleep(500000);
}

//Check if the output was created
$arr = explode(".", $files[$input_type]);
$name = $arr[0];

if(file_exists("$download_path$job/$name.$output_type")){
	echo "1";
}else{
	echo "0";
}
?>
