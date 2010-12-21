<?php
$formats = explode(',', $_GET["formats"]);
$username = "demo";
$password = "demo";
$database = "csr";

mysql_connect("localhost", $username, $password);
@mysql_select_db($database) or die("Unable to select database!");

for($i=0; $i<sizeof($formats); $i++){
	$format = trim($formats[$i]);
	$query = "SELECT files.file FROM files,formats WHERE files.valid=1 AND files.format=formats.format_id AND formats.default_extension='$format'";
	$result = mysql_query($query);

	while($row = mysql_fetch_row($result)){
		echo "$row[0]<br>\n";
	}
}

mysql_close();
?>
