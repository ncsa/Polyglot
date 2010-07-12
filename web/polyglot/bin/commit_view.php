<?php
/**
 * Commit an uploaded Polyglot view job.
 */

$ip = $_SERVER['REMOTE_ADDR'];

$upload_path = "../uploads";
$jobs_file = "../proc/jobs.txt";
$task_file = "$upload_path/$ip/tasks";
$commit_file = "$upload_path/$ip/commit";
$timestamp = "";

//Read in content types
$fp = fopen("../var/content_types.txt", "r");
$map = NULL;

while(!feof($fp)){
	$type = chop(fgets($fp));						//Read in content type

	$line = chop(fgets($fp));						//Read in formats list
	$extensions = explode(" ", $line);

	for($i=0; $i<sizeof($extensions); $i++){
		$map[$extensions[$i]] = $type;
	}

	if(strlen($line) > 0) fgets($fp);		//Read in empty line
}

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
				if($map[$formats[$i]] == '3d'){
					fwrite($fp, $formats[$i] . " obj\n");
				}else if($map[$formats[$i]] == 'image'){
					fwrite($fp, $formats[$i] . " jpg\n");
				}else if($map[$formats[$i]] == 'document'){
					fwrite($fp, $formats[$i] . " txt\n");
				}else if($map[$formats[$i]] == 'audio'){
					fwrite($fp, $formats[$i] . " wav\n");
				}
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
