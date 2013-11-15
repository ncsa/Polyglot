<?php
/**********************************************************************
 * PHP backend for Drag & Drop Upload Java Applet
 * This is Public Domain, you can use, modify or distribute it in any
 * way you wish, but please report improvements to 
 * 
 * info@bibliograph.org
 **********************************************************************/

//Constants
$upload_path = "../uploads";

//Check http basic authentication
if(!isset($_SERVER['PHP_AUTH_USER'])) 
{
  header('WWW-Authenticate: Basic realm="Upload Area"');
  header('HTTP/1.0 401 Unauthorized');
  echo '<font color=red>Access denied</font>';
  exit;
}else{
  $username = $_SERVER['PHP_AUTH_USER'];
  $password = $_SERVER['PHP_AUTH_PW'];
  
  if($username != "username" or $password != "password"){
    die("<font color=red>Wrong username or password!</font>");
  }
}

//Check if something has been uploaded 
$field_name = 'uploadfile';

if(!isset($_FILES) or !count($_FILES) or !isset($_FILES[$field_name])){
  die("<font color=red>No file data received (file might be to large).</font>");
}
  
//Check file size
$maxfilesize = 100000;	//kByte

if($_FILES['uploadfile']['size'] > $maxfilesize*1024){
  die("<font color=red>File exceeds maximum filesize: $maxfilesize kByte.</font>");
}

//Check if upload directory is writeable
if(!is_writable($upload_path)){
  die("<font color=red>Upload path is not writeable.</font>");
}

//Get file info
$tmp_name = $_FILES['uploadfile']['tmp_name'];
$file_name = $_FILES['uploadfile']['name'];

//Check file name for validity 
if(strstr($file_name, "..")){
  die("<font color=red>Illegal filename.</font>");
}

//Create uniqe id for these files
$id = $_SERVER['REMOTE_ADDR'];
//$id = date("mdy_His");

$upload_path = "$upload_path/$id";
if(!file_exists($upload_path)) mkdir($upload_path);

//Target path
$target_path = "$upload_path/$file_name";

//Move temporary file to target location and check for errors
if(!move_uploaded_file($tmp_name, $target_path)){
  die("<font color=red>Problem during upload.</font>");
}

//Report upload success
echo "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font color=green>[OK]</font>";
?>
