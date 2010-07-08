<?php
/**
 * Display information about the client IP's job position in the queue.
 */

$ip = $_SERVER['REMOTE_ADDR'];
$mode = $_GET["mode"];
if($mode == null) $mode = 1;

$upload_path = "../uploads";
$nj = 0;	//Number of jobs
$nq = 0;	//Number in the queue

//Get the modification time of our job
$job_time = filemtime("$upload_path/$ip");

//Count jobs ahead of our job
$jobs = scandir($upload_path);

for($i=0; $i<sizeof($jobs); $i++){
	if($jobs[$i] != "." && $jobs[$i] != ".."){
		$nj++;

		if($jobs[$i] != $ip){
			$mtime = filemtime("$upload_path/$jobs[$i]");
			if($mtime < $job_time) $nq++;
		}
	}
}

$nq++;

//Display results
if($mode == 1){
	echo "$nj $nq";
}else if($mode == 2){
  echo "<i><font color=#a9bfe8 size=-1>Queued: number $nq of $nj</font></i>";
}
?>
