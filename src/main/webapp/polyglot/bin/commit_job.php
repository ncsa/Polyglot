<?php
/**
 * Commit an uploaded Polyglot job.
 */

$ip = $_SERVER['REMOTE_ADDR'];
$format = $_GET["format"];

$upload_path = "../uploads";
$jobs_file = "../proc/jobs.txt";
$task_file = "$upload_path/$ip/tasks";
$commit_file = "$upload_path/$ip/commit";
$timestamp = "";

//Identify previous job for this ip (if any)
$timestamp_old = "";
$fp = fopen($jobs_file, "r");

if($fp){
  while(!feof($fp)){
    $line = fgets($fp);
    $fields = explode(" ", $line);
    
    if($fields[0] == $ip){
      $timestamp_old = $fields[1];
    }
  }

  fclose($fp);
}

if(!file_exists("$upload_path/$ip")){ //A previous commit timed out just as the daemon picked it up!
	$timestamp = $timestamp_old;
}else{
	if(!file_exists($commit_file)){			//If no previously timed out commits!
		//Set the task
		$fp = fopen($task_file, "w");

		if($fp){
  		fwrite($fp, "* $format");
	  	fclose($fp);
		}

		//Write the commit file
		$fp = fopen($commit_file, "w");
		fclose($fp);
	}

	//Wait for new jobs timestamp
	$timestamp = $timestamp_old;

	while($timestamp == $timestamp_old){
		$fp = fopen($jobs_file, "r");

  	if($fp){
    	while(!feof($fp)){
      	$line = fgets($fp);
      	$fields = explode(" ", $line);
    
      	if($fields[0] == $ip){
      	  $timestamp = $fields[1];
     	 }
    	}

    	fclose($fp);
  	}
	}
}

echo $ip . "_" . $timestamp;
?>
