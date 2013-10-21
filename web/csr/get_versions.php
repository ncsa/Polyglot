<?php
require_once("../phpsql_dbinfo.php");

$software = explode(',', $_GET["software"]);

mysql_connect("localhost", $username, $password);
@mysql_select_db($database) or die("Unable to select database!");

for($i=0; $i<sizeof($software); $i++){
	$name = mysql_real_escape_string(trim($software[$i]));
	$query = "SELECT DISTINCT software.version FROM software,software_scripts WHERE software.name REGEXP '$name' AND software.software_id=software_scripts.software";
	$result = mysql_query($query);

	if(mysql_num_rows($result) > 0){
		for($r=0; $r<mysql_num_rows($result); $r++){
			if($r > 0) echo ", ";
			echo mysql_result($result, $r, "software.version");
		}
	}else{
		echo "null";
	}

	echo "<br>\n";
}

mysql_close();
?>
