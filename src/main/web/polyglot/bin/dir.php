<?php
/**
 * Display the contents of a directory as a list.
 */

include "utils.php";

$dir = $_GET["dir"];
$filter = $_GET["filter"];
$files = scandir($dir);

for($i=0; $i<sizeof($files); $i++){
  $dot = strrpos($files[$i],'.');

  if($dot !== false){
    $ext = substr($files[$i], $dot+1);

    if($ext == $filter || $ext == "zip"){
      $size = bytes_hr(filesize("$dir/$files[$i]"));
      echo '<a href="' . $dir . '/' . $files[$i] . '" target="_top">' . $files[$i] . '</a> (' . $size . ')';
 
      if($ext == "obj"){
        echo ' <a href="model_viewer.php?file=' . $dir . '/' . $files[$i] . '&width=400&height=400" target="_blank">[view]</a>';
			}

      echo '<br>';
    }
  }
}
?>
