<!--
Display the contents of a given 3D model.
-->

<html>
<body>

<?php
$file = $_GET["file"];
$width = $_GET["width"];
$height = $_GET["height"];

if($width == null) $width = 100;
if($height == null) $height = 100;

echo '<applet archive="java/PolyglotUtils-signed.jar,http://download.java.net/media/jogl/builds/archive/jsr-231-webstart-current/jogl.jar,http://download.java.net/media/gluegen/webstart/gluegen-rt.jar" code="edu.ncsa.model.ModelViewerApplet.class" width="' . $width . '" height="' . $height . '">' . "\n";
echo '<param name="filename" value="' . $file . '">' . "\n";
echo "</applet>\n";
?>

</body>
</html>
