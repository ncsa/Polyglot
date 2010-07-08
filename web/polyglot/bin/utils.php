<?php
/*
 * Miscellaneous utility functions.
 */

function bytes_hr($bytes)
{
  $symbol = array('B', 'KB', 'MB', 'GB', 'TB');

  $exp = 0;
  $converted_value = 0;

  if($bytes > 0){
    $exp = floor(log($bytes)/log(1024));
    $converted_value = ($bytes/pow(1024,floor($exp)));
  }

  return sprintf('%.0f '.$symbol[$exp], $converted_value);
}

function scandir_bydate($dir)
{
	$files['name'] = scandir($dir);
	$files['time'] = array();
  $files_sorted = array();

	for($i=0; $i<sizeof($files['name']); $i++){
		$files['time'][$i] = filemtime($dir . '/' . $files['name'][$i]);
	}

	asort($files['time']);

	foreach($files['time'] as $key => $val){
		$files_sorted[sizeof($files_sorted)] = $files['name'][$key];
  } 

  return $files_sorted;
}
?>
