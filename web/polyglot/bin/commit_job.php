<?php
/**
 * Commit an uploaded Polyglot job.
 */

function getTask_debug($input_format, $output_format)
{
  $task = "A3DReviewer open $input_format export $output_format\n";
  return $task;
}

function getTask_IOGraph($input_format, $output_format)
{
  exec("java -cp java/PolyglotUtils-signed.jar edu.ncsa.polyglot.iograph.IOGraphViewer $input_format $output_format", $tmp);
  $task = "";

  for($i=0; $i<sizeof($tmp); $i++){
    $task = $task . $tmp[$i] . "\n";
  }

  return $task;
}

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
		//Build list of formats
		$files = scandir("$upload_path/$ip");
		$formats = array();

		for($i=0; $i<sizeof($files); $i++){
		  if($files[$i] != "." && $files[$i] != ".."){
				$dot = strrpos($files[$i],'.');

		    if($dot !== false){
		      $ext = substr($files[$i], $dot+1);
	  	    $formats[sizeof($formats)] = $ext;
		    }
		  }
		}

		//$formats = array_unique($formats);	//Buggy!
		$formats = array_keys(array_count_values($formats));

		//Set the task
		$fp = fopen($task_file, "w");

		if($fp){
  		for($i=0; $i<sizeof($formats); $i++){
	  	  //$task = getTask_debug($formats[$i], $format);
	    	$task = getTask_IOGraph($formats[$i], $format);
  		  fwrite($fp, "$task");
			}

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
