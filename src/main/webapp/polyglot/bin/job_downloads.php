<!--
Display the contents of a Polyglot job.
-->

<html>
<head>
<!--
<script language="javascript" src="tooltips.js"></script>
<link rel="stylesheet" href="tooltips.css" type="text/css" media="screen">
-->
</head>
<body link="blue" vlink="blue">

<?php
include "utils.php";

function get_path($source, $target, $tasks)
{
	$path = array();
	$path[0] = $source;

	//Iterate through tasks and accumulate conversion path
	$at = $source;

  for($i=0; $i<sizeof($tasks); $i++){
    if($tasks[$i][2] == $at){
			$path[sizeof($path)] = $tasks[$i][0];
			$path[sizeof($path)] = $tasks[$i][4];
			$at = $tasks[$i][4];
		}
	}

	//Verify this was a valid path
	if($path[sizeof($path)-1] != $target){
		$path = array();
	}

	return $path;
}

$dir = $_GET["dir"];
$target = $_GET["target"];
$files = scandir_bydate($dir);
$converted_files = array();

//Get and display a successful conversions
for($i=0; $i<sizeof($files); $i++){
  $dot = strrpos($files[$i],'.');

  if($dot !== false){
		$name = substr($files[$i], 0, $dot);
    $ext = substr($files[$i], $dot+1);

    if($ext == $target || $ext == "zip"){
			$converted_files[sizeof($converted_files)] = $name;
      $size = bytes_hr(filesize("$dir/$files[$i]"));

			if(0){	
				echo '<a href="' . $dir . '/' . $files[$i] . '" target="_top">' . $files[$i] . '</a> (' . $size . ')';
			}else{
				//This is a hack to allow things to work when called within a div one directory up!
				echo '<a href="' . substr($dir,3) . '/' . $files[$i] . '" target="_top">' . $files[$i] . '</a> (' . $size . ')';
			}

      if($ext == "obj"){
        echo ' <a href="bin/model_viewer.php?file=' . $dir . '/' . $files[$i] . '&width=400&height=400" target="_blank">[view]</a>';
			}

      echo "<br>\n";
    }
  }
}

if(file_exists($dir . '/complete')){
	//Get a list of files that failed to be converted
	$files = scandir($dir);
	$failed_files = array();

	for($i=0; $i<sizeof($files); $i++){
  	if($files[$i] != "." && $files[$i] != ".."){
  		$dot = strrpos($files[$i],'.');

			if($dot !== false){
				$name = substr($files[$i], 0, $dot);
    		$ext = substr($files[$i], $dot+1);

				if($ext != "zip"){
    			if(!in_array($name, $converted_files)){
						if(!in_array($name, $failed_files)){
							$failed_files[sizeof($failed_files)] = $name;
						}
					}
    		}
			}
		}
	}

	//Load tasks
	$tasks = array();
	$h = fopen("$dir/tasks", "r");

	if($h){
		while(!feof($h)){
			$line = trim(fgets($h));

			if(!empty($line)){
				$tasks[sizeof($tasks)] = explode(" ", $line);
			}
		}
	}

	fclose($h);
	
	//Load converters
	$converters = array();
	$h = fopen("../var/iograph/converters.txt", "r");

	if($h){
		while(!feof($h)){
			$line = trim(fgets($h));

			if(!empty($line)){
				$tmp = explode("=", $line);
				$converters[$tmp[0]] = $tmp[1];
			}
		}
	}

	fclose($h);

	//Display failed files
	foreach($failed_files as $name){
		$blame = "";
		$last = "";
		$next = "";

		$source = "";
		$path = "";
		$blame_predessesor;

		//Attempt to assign blame
		for($i=sizeof($tasks)-1; $i>=0; $i--){
			$file_in = $dir . '/' . $name . '.' . $tasks[$i][2];
			$file_out = $dir . '/' . $name . '.' . $tasks[$i][4];

			if(file_exists($file_in) && !file_exists($file_out)){
				$blame = $converters[$tasks[$i][0]];
				$last = $tasks[$i][2];
				$next = $tasks[$i][4];
				break;
			}
		}

		if(!empty($last)){
			//Determine original file type
			$source = $last;
			$FOUND = true;

  		while($FOUND){
				$FOUND = false;

    		for($i=sizeof($tasks)-1; $i>=0; $i--){
      		if($tasks[$i][4] == $source){
						if(file_exists($dir . '/' . $name . '.' . $tasks[$i][2])){
							$source = $tasks[$i][2];
							$FOUND = true;
							break;
						}
					}
    		}
  		}

			//Get conversion path
			$path = get_path($source, $target, $tasks);

			for($i=1; $i<sizeof($path); $i+=2){
				$path[$i] = $converters[$path[$i]];
			}

			//Find the blamed applications predesessor
			if(sizeof($path) > 1){
				$blame_predessesor = $path[1];

				for($i=3; $i<sizeof($path); $i+=2){
					if($blame == $path[$i] && $last == $path[$i-1]){
						break;
					}

					$blame_predessesor = $path[$i];
				}
			}
		}else{
			//No conversion path, find original type by brute force search
			for($i=0; $i<sizeof($files); $i++){
  			$dot = strrpos($files[$i],'.');

				if($dot !== false){
					$tmp = substr($files[$i], 0, $dot);

					if($name == $tmp){
   					$source = substr($files[$i], $dot+1);
						break;
					}
				}
			}
		}

		//Build output
		if(empty($path)){
			$buffer = "A conversion path between <b><font color=red>$source</font></b> and <b><font color=blue>$target</font></b> was <b>not found</b>.";
		}else{
			$buffer = "A <u>conversion path</u> was found:<br><br>";

			for($i=0; $i<sizeof($path); $i++){
				$buffer .= "<b><font color=";

				if($i == 0){
					$buffer .= "red";
				}else if($i == sizeof($path)-1){
					$buffer .= "blue";
				}else if($i%2 == 1){
					$buffer .= "black";
				}else{
					$buffer .= "purple";
				}

				$buffer .= ">" . $path[$i] . "</font></b>";

				if($i < sizeof($path)-1){
					$buffer .= " &rarr; ";
				}
			}

			if($path[0] == $last){
				$buffer .= "<br><br>However, <b>$blame</b> failed to load the uploaded file <b>$name.<font color=red>$last</font></b>.";
			}else{
				$buffer .= "<br><br>However, <b>$blame</b> failed to load the intermediary file <b>$name.<font color=purple>$last</font></b> created from <b>$blame_predessesor</b>.";
			}
		}

		//Display output
		echo "<s><font color=\"red\">" . $name . "." . $target . "</font></s>";
		//echo " <a href=\"#\" title=\"" . $buffer . "\" class=\"tip\"><font color=\"red\">[why?]</font></a>";
		echo " <a href=\"#\" class=\"tip\"><font color=\"red\">[why?]</font><span>" . $buffer . "</span></a>";
    echo "<br>\n";
  }
}
?>

</body>
</html>
