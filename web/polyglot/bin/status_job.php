<?php
/**
 * Display information about a specified job (i.e. successes/failures).
 */

$dir = $_GET["dir"];
$n0 = $_GET["n0"];
$output = $_GET["output"];
$output = explode(",", $output);

$files = scandir("../downloads/$dir");
$n1 = 0;
$jc = false;
$az = false;

for($i=0; $i<sizeof($files); $i++){
	if($files[$i] == "complete"){
		$jc = true;
	}else if($files[$i] == "all.zip"){
		$az = true;
	}else{
  	$dot = strrpos($files[$i],'.');

  	if($dot !== false){
			$ext = substr($files[$i], $dot+1);

			if(in_array($ext, $output)){
				$n1++;
			}
		}
	}
}

$nf = $n0-$n1;

if($jc){
	echo "<i><font color=#a9bfe8 size=-1>Succeeded: $n1, Failed: $nf</font></i>";

	if(!$az){
		$path = "../downloads/$dir";
		exec("zip -j $path/all.zip $path/*.$output[0]");
	}
}else{
	echo "<i><font color=#a9bfe8 size=-1>Succeeded: $n1 ";
	//$tmp = rand();
	$tmp = time();

	if($tmp % 4 == 0){
		//echo "|";
		echo ".";
	}else if($tmp % 4 == 1){
		//echo "/";
		echo "..";
	}else if($tmp % 4 == 2){
		//echo "-";
		echo "...";
	}else{
		//echo "\\";
		echo "....";
	}

	echo "</font></i>";
}
?>
