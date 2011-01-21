<html>
<head>
<script language="javascript" src="bin/tooltips.js"></script>
<link rel="stylesheet" href="bin/tooltips.css" type="text/css" media="screen">
<title>NCSA Polyglot</title>
</head>
<body link="blue" vlink="blue">

<div style="float:left;">
<a href="iograph.php">Conversion Graph</a>
&nbsp;
<b>Convert</b>
&nbsp;
<a href="view.php">View</a>
<?php
include "bin/utils.php";

$server_url = "http://" . $_SERVER['SERVER_ADDR'] . ":8183/distributed_software/";

if(url_exists($server_url . "alive")){
	echo "&nbsp;\n";
	echo "<a href=\"" . $server_url . "form\"><i>Software</i></a>\n";
}
?>
</div>

<div style="float:right;">
<img src="images/blank.jpg" width="2">
<a href="http://www.archives.gov"><img src="images/nara_smooth.png" height="25" border="0"></a>
<a href="http://isda.ncsa.illinois.edu"><img src="images/isda_smooth.png" height="25" border="0"></a>
<a href="http://www.ncsa.illinois.edu"><img src="images/ncsa2_smooth.png" height="25" border="0"></a>
<a href="http://illinois.edu"><img src="images/uofi.png" height="25" border="0"></a>
<!-- <a href="http://www.chass.illinois.edu"><img src="images/ichass_smooth.png" height="20" border="0"></a> -->
<br>
<div style="float:right;">
<i><font size="-2"><a href="misc/PolyglotManual.pdf">Polyglot Manual</a></font></i>
</div>
</div>

<br>
<hr style="border:0; height:1px; margin-top:4px; color:#c9d7f1; background-color:#c9d7f1;">
<iframe height="30" frameborder="0" scrolling="no" src="bin/auto_refresh.php?refresh=500&auto_refresh_file=status_system.php?mode=2"></iframe>
<br>

<center>

<!-- Title -->
<!--
<div style="position:relative; top:20px; left:-58px;">
<img src="images/ncsa-logo.gif">
<div style="position:relative; top:-34px; left:120px;">
<font size="+3"><b>&nbsp;&nbsp;&nbsp;&nbsp;Polyglot</b></font>
</div></div>
-->
<div style="position:relative; left:-25px;">
<img src="images/ncsa_polyglot_smooth.jpg" title="(Wikipedia): adj. someone who aptly and with a high level of fluency uses many languages." width="400" border="0">
</div>
<br>

<table><tr><td>

<table style="border-spacing:0px">
<tr><td>
<div style="background-color:#6a82db; color:#ffffff; width:150; height:194; padding:5; padding-top:1;">
<ol style="font-weight:bold; font-size:14; padding-left:25;">
<li>Drag files to upload area.<br><br>
<li>Select output format.<br><br>
<li>Click "Convert" button.
</ol>
</div>

</td><td>

<!-- Upload Applet -->
<applet name="uploadApplet" code="dndapplet.applet.DNDApplet.class" archive="lib/PolyglotUtils-signed.jar"
				mayscript="true" scriptable="true" width="325" height="200">
<param name="upload_button_text" value="Convert">
<param name="uploadPath" value="bin/upload.php">
<param name="funcNameHandleError" value="handleError">
<param name="funcNameHandleStartUpload" value="handleStartUpload">
<param name="funcNameHandleCurrentUpload" value="handleCurrentUpload">
<param name="funcNameHandleEndUpload" value="handleEndUpload">
<param name="funcNameHandleStringMimeType" value="handleStringMimeType">
<param name="funcNameHandleUnknownMimeType" value="handleUnkownMimeType">

<!-- The form will be displayed if the browser cannot display applets. -->
<form enctype="multipart/form-data" action="bin/upload.php" method="post">
Data: <input name="uploadfile" type="file" size="25"> <input type="submit">
</form>
</applet>

<!--
<div>
User name: 
<input id="username" value="username" onchange="setUsername(this.value);">
</div>

<div>
Password: 
<input id="password" value="password" onchange="setPassword(this.value);">
</div>
-->

</td><td>

<!-- Options List -->
<?php
$filter = $_GET["filter"];
$map = NULL;

echo '<table><tr><td>' . "\n";

//Read in content types
$fp = fopen("var/content_types.txt", "r");
echo '<select style="width:75px">' . "\n";
echo '<option value="all" onclick="top.location.replace(\'convert.php?filter=all\')">all</option>\n"';

while(!feof($fp)){
	$type = chop(fgets($fp));						//Read in content type
	echo '<option value="' . $type. '" onclick="top.location.replace(\'convert.php?filter=' . $type . '\')"';
	if($filter == $type) echo ' selected';
	echo '>' . $type. "</option>\n";

	$line = chop(fgets($fp));						//Read in formats list
	$extensions = explode(" ", $line);

	for($i=0; $i<sizeof($extensions); $i++){
		$map[$extensions[$i]] = $type;
	}

	if(strlen($line) > 0) fgets($fp);		//Read in empty line
}

echo '</select>' . "\n";
fclose($fp);

echo '</td></tr><tr><td>' . "\n";

//Read in supported output formats
$fp = fopen("var/output_formats.txt", "r");
echo '<select id="format" size="4" style="width:75px; height:176px">' . "\n";

while(!feof($fp)){
  $line = chop(fgets($fp));

	if(strlen($line) > 0 && ($filter == NULL || $filter == "all" || $filter == $map[$line])){
  	if($line == "obj"){
    	echo '<option value="' . $line . '" selected="selected">' . $line . "</option>\n";
  	}else{
    	echo '<option value="' . $line . '">' . $line . "</option>\n";
		}
	}
}

echo '</select>' . "\n";
fclose($fp);

echo '</td></tr></table>' . "\n";
?>

</td></tr>
</table>

</td></tr><tr><td align="center">

<div id="statusLabel">
<div style="float:left;">
<i><font size="-2">* File names must not contain spaces.</font></i>
</div>
</div>

</td></tr></table>

<br>
<img name="download_arrow" id="download_arrow" style="display:none" src="images/arrow.gif">

<!-- Download Frame -->
<br><br>
<div id="resultsLinks"></div>
	
<table style="border-spacing:0px;"><tr><td name="download_help_td" id="download_help_td" bgcolor="#ffffff" valign="top">
<div name="download_help" id="download_help" style="background-color:#ffffff; color:#ffffff; width:150; height:190; padding:5; padding-top:1;">
<ol start="4" style="font-weight:bold; font-size:14; padding-left:25;">
<li>Download converted files.
</ol>
</div>
</td><td>
<div name="downloads" id="downloads" style="width:404; height:190;"></div>
</td></tr></table>

<iframe name="progress" id="progress" width="404" height="35" frameborder="0" scrolling="no"></iframe>
</center>

<script>

setUsername("username");
setPassword("password");

String.prototype.trim = function () {
	return this.replace(/^\s*/, "").replace(/\s*$/, "");
}
         
//Sets the username for basic http authentication
//@param {String} prefix
function setUsername(username)
{
  document.uploadApplet.setUsername(username);
}

//Sets the password for basic http authentication
//@param {String} prefix
function setPassword(password)
{
  document.uploadApplet.setPassword(password);
}
         
//Called when upload starts with number of files to transmit
//@param {int} number
function handleStartUpload(number)
{
  window._number = number;
  window._index = 1;
}
         
//Called when a file is uploaded
//@param {String} filename
function handleCurrentUpload(filename)
{
  msg = "Uploading " + filename + " (" + (window._index++) + "/" + window._number + ")...";
  document.getElementById("statusLabel").innerHTML = msg;
}

//Make calls to server
function AJAXCall(file)
{
  xmlHttp = new XMLHttpRequest();
	if(xmlHttp == null){alert("Your browser does not support AJAX!"); return;}
  xmlHttp.open("GET", file, false);
  xmlHttp.send(null);
  buff = xmlHttp.responseText;

  return buff;
}

var lowerpane_url = "";

function autoRefreshLowerPane()
{
	if(lowerpane_url.substring(lowerpane_url.length-3, lowerpane_url.length) == "log"){
		document.getElementById('downloads').innerHTML = "<pre>" + AJAXCall(lowerpane_url) + "</pre>";
	}else{
		document.getElementById('downloads').innerHTML = AJAXCall(lowerpane_url);
	}

	setTimeout('autoRefreshLowerPane()', 1000);
}

//Called when upload is finished
function handleEndUpload()
{
	document.getElementById("statusLabel").innerHTML = "Upload finished";

	//Show downloads pane
  document.getElementById('download_arrow').setAttribute('style', 'display:inline');
  document.getElementById('download_help_td').setAttribute('bgcolor', '#6a82db');
  document.getElementById('download_help').setAttribute('style', 'background-color:#6a82db; color:#ffffff; width:150; height:190; padding:5; padding-top:1;');
  document.getElementById('downloads').setAttribute('style', 'width:404; height:auto; min-height:190; border-style:inset; border-width:2; border-color:#a9bfe8; overflow:visible;');
	
	//Set links as disabled
  options = "<b><font color=#aaaaaa>Downloads</font></b>";
  options += " | ";
  options += "<b><font color=#aaaaaa>Details</font></b>";
	options += "\n<br>\n";
	
	document.getElementById('resultsLinks').innerHTML = options;
	
	//Show queue status
  document.getElementById('progress').src = "bin/auto_refresh.php?refresh=500&auto_refresh_file=status_queue.php?mode=2";
	
	//Commit job (and wait for daemon to timestamp folder)
	var timestamp = "";

	while(timestamp == ""){	//Re-commit if timed out!
		timestamp = AJAXCall("bin/commit_job.php?format=" + document.getElementById("format").value);
	}

	timestamp = timestamp.trim();

  //Show progress
  document.getElementById('progress').src = "bin/auto_refresh.php?refresh=500&auto_refresh_file=status_job.php&dir=../downloads/" + timestamp + "&n0=" + window._number + "&output=" + document.getElementById("format").value;
	
	//Set contents of downloads pane
	lowerpane_url = "bin/job_downloads.php?dir=../downloads/" + timestamp + "&target=" + document.getElementById("format").value;
	autoRefreshLowerPane();

	//Set links
  style_toggle1 = "onclick=\"";
  style_toggle1 += "lowerpane_url = 'bin/job_downloads.php?dir=../downloads/" + escape(timestamp) + "&target=" + document.getElementById("format").value + "';";
  style_toggle1 += "document.getElementById('download_link').style.color = '#000000';";
  style_toggle1 += "document.getElementById('status_link').style.color = '#0000ff';";
  style_toggle1 += "document.getElementById('download_link').style.fontWeight = '700';";
  style_toggle1 += "document.getElementById('status_link').style.fontWeight = '400';";
  style_toggle1 += "document.getElementById('download_link').style.textDecoration = 'none';";
  style_toggle1 += "document.getElementById('status_link').style.textDecoration = 'underline';";
  style_toggle1 += "\"";

	style_toggle2 = "onclick=\"";
	style_toggle2 += "lowerpane_url = 'downloads/" + escape(timestamp) + "/log';";
  style_toggle2 += "document.getElementById('download_link').style.color = '#0000ff';";
  style_toggle2 += "document.getElementById('status_link').style.color = '#000000';";
  style_toggle2 += "document.getElementById('download_link').style.fontWeight = '400';";
  style_toggle2 += "document.getElementById('status_link').style.fontWeight = '700';";
  style_toggle2 += "document.getElementById('download_link').style.textDecoration = 'underline';";
  style_toggle2 += "document.getElementById('status_link').style.textDecoration = 'none';";
  style_toggle2 += "\"";

  style_selected = "style=\"text-decoration:none; color:black; font-weight:700;\"";

  options = "<a href=\"#\" id=\"download_link\" " + style_toggle1 + " " + style_selected + ">Downloads</a>";
	options += " | ";
  options += "<a href=\"#\" id=\"status_link\" " + style_toggle2 + ">Details</a>";
	options += "\n<br>\n";

  document.getElementById('resultsLinks').innerHTML = options;
}

//Handle errors
//@param {String} error message
function handleError(message)
{
  document.getElementById("statusLabel").innerHTML = message;
}

//Handle mime types that can be represented as a string
//@param {Object} mimeType, @param {Object} string
function handleStringMimeType(mimeType, string)
{
  alert("String Mime-Type " + mimeType + ": '"  + string + "'");
}

//Handle mime types that cannot be represented as string data
//@param {Object} mimeType, @param {Object} data
function handleUnkownMimeType(mimeType, data)
{
  alert("Unknown Mime-Type " + mimeType + ": "  + data);
}

</script>
</body>
</html>
