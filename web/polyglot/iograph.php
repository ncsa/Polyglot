<html>
<head>
<title>NCSA Polyglot</title>
</head>
<body link="blue" vlink="blue">

<div style="float:left;">
<b>Conversion Graph</b>
&nbsp;
<a href="convert.php">Convert</a>
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
</div>

<br>
<hr style="border:0; height:1px; margin-top:4px; color:#c9d7f1; background-color:#c9d7f1;">

<div style="text-align:right;">
<img src="images/ncsa_polyglot_smooth.jpg" width="300">
</div>

<table border="0">
<tr><td>
<div style="background-color:#6a82db; color:#ffffff; width:200; height:700; padding:5;">
<b>
Use the I/O-Graph to check which applications will be used when converting between a source and target file format.
</b>
<ol style="font-weight:bold; font-size:14; padding-left:25;">
<li>Right click on the source format and select "Source" from the popup menu.<br><br>
<li>Right click on the target format and select "Target" from the popup menu.<br><br>
</ol>
</div>
</td><td width="100%" align="center">

<applet codebase="lib" archive="PolyglotUtils-signed.jar" code="edu.ncsa.icr.polyglot.IOGraphApplet.class" width="900" height="650">
<param name="url" value="<?php echo $_SERVER['SERVER_ADDR']?>:50002">
<param name="side_pane_width" value="275">
<param name="output_panel_height" value="0">
<param name="vertex_color" value="000000">
<param name="edge_color" value = "cccccc">
OOPs, no java support.
</applet>
</td></tr>
</table>

</body>
</html>
