<?php
/**
 * Return 1 if the specified file exists and 0 otherwise.
 */

$file = $_GET["file"];

if(file_exists($file)){
  echo "1";
}else{
  echo "0";
}
?>
