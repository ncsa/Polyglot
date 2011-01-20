<html>
<head>
<title>NCSA Polyglot</title>
</head>
<body link="blue" vlink="blue">

<div style="float:left;">
<a href="iograph.php">Conversion Graph</a>
&nbsp;
<a href="convert.php">Convert</a>
&nbsp;
<b>View</b>
<?php
if(file_exists("SHOW_SOFTWARE.txt")){
	echo "&nbsp;\n";
	echo "<a href=\"http://" . $_SERVER['SERVER_ADDR'] . ":8183/distributed_software/form\"><i>Software</i></a>\n";
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
<i><font size="-2"><a href="http://kenai.com/projects/jogl/pages/Home">Java OpenGL (Required to view 3D!)</a></font></i>
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
<img src="images/ncsa_polyglot_smooth.jpg" width="400">
</div>
<br>

<table style="border-spacing:0px">
<tr><td>
<div style="background-color:#6a82db; color:#ffffff; width:200; height:194; padding:5; padding-top:1;">
<b>
Preview anything that can be converted to a viewable format.
</b>
<ol style="font-weight:bold; font-size:14; padding-left:25;">
<li>Drag files to upload area.<br><br>
<li>Click "View" button.
</ol>
</div>
</td><td>

<!-- Upload Applet -->
<applet name="uploadApplet" code="dndapplet.applet.DNDApplet.class" archive="lib/PolyglotUtils-signed.jar"
				mayscript="true" scriptable="true" width="400" height="200">
<param name="upload_button_text" value="View">
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

<!--
<form>
<input type="hidden" id="format" value="obj">
</form>
-->

</td></tr>
</table>

<!-- <br><br style="font-size: 1px; line-height: 1;"> -->
<iframe name="progress" id="progress" width="398" height="35" frameborder="0" scrolling="no"></iframe>
<center><div id="statusLabel"></div></center>

<!-- Download Area -->
<br><br>
<iframe id="viewarea" width="100%" height="40%" frameborder="0"></iframe>
</center>

<script>

setUsername("username");
setPassword("password");
         
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

//Called when upload is finished
function handleEndUpload()
{
	document.getElementById("statusLabel").innerHTML = "Upload finished";

	//Show queue status
  document.getElementById('progress').src = "bin/auto_refresh.php?refresh=500&auto_refresh_file=status_queue.php?mode=2";

	//Commit job (and wait for daemon to timestamp folder)
	var timestamp = "";

	while(timestamp == ""){	//Re-commit if timed out!
		//timestamp = AJAXCall("bin/commit_job.php?format=" + document.getElementById("format").value);
		timestamp = AJAXCall("bin/commit_view.php");
	}

	//document.getElementById('progress').src = "bin/auto_refresh.php?refresh=500&auto_refresh_file=status_job.php&dir=../downloads/" + timestamp + "&n0=" + window._number + "&output=" + document.getElementById("format").value;
	document.getElementById('progress').src = "bin/auto_refresh.php?refresh=500&auto_refresh_file=status_job.php&dir=../downloads/" + timestamp + "&n0=" + window._number + "&output=obj,jpg,txt,wav";
	document.getElementById('viewarea').src = "bin/dir_mosaic.php?dir=../downloads/" + timestamp;

	document.getElementById("statusLabel").innerHTML = "";
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
