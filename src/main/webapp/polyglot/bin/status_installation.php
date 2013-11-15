<!--
Display information about the status of the Polyglot installation.
-->

<html>
<body>
<center>
<h1><i>Polyglot Setup Diagnostics</i></h1>
<br>

<?php
//Read in samples
$samples = scandir("../misc/samples");
$files = array();

//Build extension->file map
for($i=0; $i<sizeof($samples); $i++){
	if($samples[$i] != "." && $samples[$i] != ".."){
		$arr = explode(".", $samples[$i]);
		$files[$arr[1]] = $samples[$i];
	}
}

//Read in IOGraph fields
$fp = fopen("../var/iograph/fields.txt", "r");
$inputs = null;
$outputs = null;

while(!feof($fp)){
	$line = fgets($fp);
	$arr = explode("=", $line);

	if($arr[0] == "InputFields"){
		$inputs = explode(",", trim($arr[1]));
	}else if($arr[0] == "OutputFields"){
		$outputs = explode(",", trim($arr[1]));
	}
}

fclose($fp);

//Read in IOGraph application registry
$fp = fopen("../var/iograph/conversions.txt", "r");
$name = "";
$alias = "";
$input_operation = "";
$input_type = "";
$output_operation = "";
$output_type = "";
$applications = array();

while(!feof($fp)){
  $line = fgets($fp);
	$arr = explode(": ", $line);

	//Set the parts needed for an application test
  if($arr[0] == "Name"){
		$name = "";
		$alias = "";
		$input_operation = "";
		$input_type = "";
		$output_operation = "";
		$output_type = "";
	
		if(!array_key_exists(trim($arr[1]), $applications)){	
			$name = trim($arr[1]);
		}
	}else if($arr[0] == "Alias"){
		$alias = trim($arr[1]);
	}else if($input_operation == "" && in_array($arr[0], $inputs)){
		$types = explode(", ", $arr[1]);

		for($i=0; $i<sizeof($types); $i++){
			if(array_key_exists(trim($types[$i]), $files)){
				$input_operation = $arr[0];
				$input_type = trim($types[$i]);
				break;
			}
		}
	}else if($output_operation == "" && in_array($arr[0], $outputs)){
		$types = explode(", ", $arr[1]);

		if(trim($types[0]) != ""){
			$output_operation = $arr[0];
			$output_type = trim($types[0]);
		}
	}

	//Check for a completed test
	if($name != "" && $alias != "" && $input_operation != "" && $output_operation != ""){
		//Set test job and prevent further tests of this application
		$applications[$name] = "$alias $input_operation $input_type $output_operation $output_type";
		$name = "";
	}
}

fclose($fp);
?>

<script language="javascript">
var tasks = new Array();
var attempts = new Array();
var successes = new Array();

<?php
$keys = array_keys($applications);

for($i=0; $i<sizeof($keys); $i++){
	echo "tasks[$i]=\"" . $applications[$keys[$i]] . "\";\n";
	echo "attempts[$i]=0;\n";
	echo "successes[$i]=0;\n";
}
?>

var TESTING = false;

function test_task(i)
{
	if(TESTING) return 0;
	TESTING = true;

	document.getElementById("button" + i).disabled = true;
  attempts[i]++;

	xmlHttp = new XMLHttpRequest();
	if(xmlHttp == null){ alert("Your browser does not support AJAX!"); return; }
	xmlHttp.open("GET", "test_task.php?task=" + tasks[i], false);
	xmlHttp.send(null);
	buffer = xmlHttp.responseText;

	successes[i] += parseInt(buffer);

	if(successes[i] == 0){
		document.getElementById("label" + i).style.background = "lightpink";
	}else if(successes[i] == attempts[i]){
		document.getElementById("label" + i).style.background = "lightgreen";
	}else{
		document.getElementById("label" + i).style.background = "lightyellow";
	}

  document.getElementById("output" + i).innerHTML = successes[i] + " of " + attempts[i] + " successful";
	document.getElementById("button" + i).disabled = false;
	TESTING = false;

	return 1;
}

function sleep(milliseconds)
{
	var start = new Date().getTime();

	while(1){
		if((new Date().getTime()-start) > milliseconds){
			break;
		}
	}
}

function test_all()
{
  var i = 0;
	
	document.getElementById("buttonAll").disabled = true;

	while(i<tasks.length){
    if(test_task(i)) i++;
		sleep(500);
  }
	
	document.getElementById("buttonAll").disabled = false;
}

</script>

<table border=1 width=50% style=border-style:none;>
<tr><td align=center style=padding:0><div style=background-color:white;><b>All</b></div></td></tr>
<tr><td style=padding:0;><table border=1 width=100% style=border-collapse:collapse;>
<tr><td width=25% align=center><button id="buttonAll" style="width:100%;" onclick="test_all()"><b>Test</b></button></td><td align=center>&nbsp;&nbsp;Test installations of all scripted applications.</td></tr>
</td></tr></table>
</table><br>

<?php
//Build page
for($i=0; $i<sizeof($keys); $i++){
	echo "<table border=1 width=50% style=border-style:none;>\n";
  echo "<tr><td align=center style=padding:0><div id=\"label$i\" style=background-color:lightgray;><b>" . $keys[$i] . "</b></div></td></tr>\n";
	echo "<tr><td style=padding:0;><table border=1 width=100% style=border-collapse:collapse;>\n";
  echo "<tr><td width=25% align=center><b>Command</b></td><td align=center><font face=courier>" . $applications[$keys[$i]] . "</font></td></tr>\n";
  echo "<tr><td align=center><button id=\"button$i\" style=\"width:100%;\" onclick=\"test_task(" . $i . ")\"><b>Test</b></button></td><td align=center><div id=\"output$i\">0 of 0 successful</div></td></tr>\n";
	echo "</td></tr></table>\n";
	echo "</table><br>\n\n";
}
?>

</center>
</body>
</html>
