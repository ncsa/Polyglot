<?php
$software = explode(',', $_GET["software"]);
$username = "demo";
$password = "demo";
$database = "csr";

mysql_connect("localhost", $username, $password);
@mysql_select_db($database) or die("Unable to select database!");

for($i=0; $i<sizeof($software); $i++){
	$name = trim($software[$i]);
	$query = "SELECT files.file FROM software,software_scripts,files WHERE software.name REGEXP '$name' AND software.software_id=software_scripts.software AND files.file_id=software_scripts.file";
	$result = mysql_query($query);

	while($row = mysql_fetch_row($result)){
		echo "$row[0]<br>\n";
	}
}

mysql_close();
?>
