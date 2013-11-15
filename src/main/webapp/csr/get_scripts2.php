<?php
require_once("../phpsql_dbinfo.php");

$software = explode(',', $_GET["software"]);
$software_keys = $_GET["software_keys"];

mysql_connect("localhost", $username, $password);
@mysql_select_db($database) or die("Unable to select database!");

for($i=0; $i<sizeof($software); $i++){
	$tmp = trim($software[$i]);
	$ind = strpos($tmp, "(");

	if($ind === false){
		$name = mysql_real_escape_string($tmp);
		$query = "SELECT software.software_id, files.file FROM software,software_scripts,files WHERE software.name REGEXP '$name' AND software.software_id=software_scripts.software AND files.file_id=software_scripts.file";
	}else{
		$name = mysql_real_escape_string(trim(substr($tmp, 0, $ind)));	
		$version = mysql_real_escape_string(trim(substr($tmp, $ind+1, strlen($tmp)-$ind-2)));
		$query = "SELECT software.software_id, files.file FROM software,software_scripts,files WHERE software.name REGEXP '$name' AND software.version='$version' AND software.software_id=software_scripts.software AND files.file_id=software_scripts.file";
	}

	$result = mysql_query($query);

	while($row = mysql_fetch_row($result)){
		if($software_keys == "true"){
			echo "$row[0]/$row[1]<br>\n";
		}else{
			echo "$row[1]<br>\n";
		}
	}
}

mysql_close();
?>
