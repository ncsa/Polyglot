<?php
$username = "demo";
$password = "demo";
$database = "csr";

mysql_connect("localhost", $username, $password);
@mysql_select_db($database) or die("Unable to select database!");

$query = "SELECT DISTINCT software.name FROM software,software_scripts WHERE software.software_id=software_scripts.software ORDER BY software.name";
$result = mysql_query($query);

while($row = mysql_fetch_row($result)){
	echo "$row[0]<br>\n";
}

mysql_close();
?>
