<?php
/**
 * Display information about the Polyglot servers current load.
 */

include "utils.php";

$mode = $_GET["mode"];
if($mode == null) $mode = 0;

$nj = 0;	//Number of jobs
$nf = 0;	//Number of files
$nb = 0;	//Number of bytes

//Read in jobs
$dir = "../uploads";
$jobs = scandir($dir);

for($i=0; $i<sizeof($jobs); $i++){
	if($jobs[$i][0] != '.' && is_dir("$dir/$jobs[$i]")){
    $files = scandir($dir . '/' . $jobs[$i]);
    $nj++;

		for($j=0; $j<sizeof($files); $j++){
			if($files[$j] != '.' && $files[$j] != ".." && $files[$j] != "tasks" && $files[$j] != "commit"){
				$size = bytes_hr(filesize("$dir/$jobs[$i]/$files[$j]"));
        $nf++;
				$nb += $size;

				if($mode == 0){
	        echo $jobs[$i] . '/' . $files[$j] . ' (' . $size . ')';
					echo '<br>';
				}
			}
		}
	}
}

//Display results
if($mode == 1){
	echo "$nj $nf $nb";
}else if($mode == 2){
  echo "<i><font color=#a9bfe8 size=-1>Jobs: $nj, Files: $nf, Bytes: " . bytes_hr($nb) . "</font></i>";
}
?>
