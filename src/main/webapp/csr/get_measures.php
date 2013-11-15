<?php
$username = "demo";
$password = "demo";
$database = "csr";

mysql_connect("localhost", $username, $password);
@mysql_select_db($database) or die("Unable to select database!");

$query = "SELECT measures.name FROM measures";
$result = mysql_query($query);

while($row = mysql_fetch_row($result)){
	echo "$row[0]<br>\n";
}

mysql_close();
?>
