<?php
$username = "demo";
$password = "demo";
$database = "csr";

mysql_connect("localhost", $username, $password);
@mysql_select_db($database) or die("Unable to select database!");

$query = "SELECT software.name, inputs.default_extension, outputs.default_extension FROM conversions, software, formats AS inputs, formats AS outputs WHERE conversions.software=software.software_id AND conversions.input_format=inputs.format_id AND conversions.output_format=outputs.format_id";
$result = mysql_query($query);

while($row = mysql_fetch_row($result)){
	echo "$row[0] $row[1] $row[2]<br>\n";
}

mysql_close();
?>
