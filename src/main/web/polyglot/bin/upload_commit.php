<?php
/**
 * Upload a file and commit it as a job to the Polyglot server.
 */

$format = $_REQUEST["format"];
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
    die("Wrong username or password!");
  }
}

//Check if something has been uploaded 
$field_name = 'uploadfile';

if(!isset($_FILES) or !count($_FILES) or !isset($_FILES[$field_name])){
  die("No file data received (file might be to large).");
}
  
//Check file size
$maxfilesize = 100000;	//kByte

if($_FILES['uploadfile']['size'] > $maxfilesize*1024){
  die("File exceeds maximum filesize: $maxfilesize kByte.");
}

//Check if upload directory is writeable
if(!is_writable($upload_path)){
  die("Upload path is not writeable.");
}

//Get file info
$tmp_name = $_FILES['uploadfile']['tmp_name'];
$file_name = $_FILES['uploadfile']['name'];

//Check file name for validity 
if(strstr($file_name, "..")){
  die("Illegal filename.");
}

//Create unique id for these files
$id = $_SERVER['REMOTE_ADDR'] . "_" . md5(uniqid());

$upload_path = "$upload_path/$id";
if(!file_exists($upload_path)) mkdir($upload_path);

//Target path
$target_path = "$upload_path/$file_name";

//Move temporary file to target location and check for errors
if(!move_uploaded_file($tmp_name, $target_path)){
  die("Problem during upload.");
}

$task_file = "$upload_path/tasks";
$commit_file = "$upload_path/commit";

//Set the task
$fp = fopen($task_file, "w");

if($fp){
	fwrite($fp, "* $format");
	fclose($fp);
}

//Write the commit file
$fp = fopen($commit_file, "w");
fclose($fp);

//Return the folder name (i.e. id)
echo $id;
?>
