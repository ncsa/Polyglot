<!--
Display the contents of a given file.
-->

<html>
<body link="blue" vlink="blue">

<center>
<div id="output" align="left"></div>
</center>

<script language="JavaScript">
<?php
//Set PHP variables
$file = $_GET["file"];
$refresh = $_GET["refresh"];
if($refresh == null) $refresh = 1000;
$tmp = explode(".", $file);
$ext = $tmp[sizeof($tmp)-1];
?>

function AJAXCall(file)
{
  xmlHttp = new XMLHttpRequest();
  if(xmlHttp == null){ alert("Your browser does not support AJAX!"); return; }
  xmlHttp.open("GET", file, false);
  xmlHttp.send(null);
  buff = xmlHttp.responseText;

  return buff;
}

function reloadPosts()
{
  //Check if file exists
  <?php
  echo 'var tmp = "exists.php?file=' . $file . '";' . "\n";
  ?>

  if(AJAXCall(tmp) == "1"){
    <?php
    if($ext == "obj"){
      $width = $_GET["width"];
      $height = $_GET["height"];

      if($width == null) $width = 100;
      if($height == null) $height = 100;

      echo 'var tmp = "model_viewer.php?file=' . $file . '&width=' . $width . '&height=' . $height . '";' . "\n";
			echo 'document.getElementById("output").innerHTML = AJAXCall(tmp);' . "\n";
		}else if($ext == "jpg"){
      $width = $_GET["width"];
      $height = $_GET["height"];

			if($width == null) $width = 100;
			if($height == null) $height = 100;

      echo 'document.getElementById("output").innerHTML = \'<img src="' . $file . '" width="' . $width . '" height="' . $height . '">\'' . "\n";
		}else if($ext == "wav"){
      $width = $_GET["width"];
      $height = $_GET["height"];

			if($width == null) $width = 100;
			if($height == null) $height = 100;

      echo 'document.getElementById("output").innerHTML = \'<embed src="' . $file . '" autostart="false" width="' . $width . '" height="' . $height . '"></embed>\'' . "\n";
		}else{
			echo 'var tmp = "' . $file . '";' . "\n";
    	echo 'var buff = AJAXCall(tmp);';

			if($ext == "txt"){
      	echo 'document.getElementById("output").innerHTML = \'<pre>\' + buff + \'</pre>\'' . "\n";
    	}else{
      	echo 'document.getElementById("output").innerHTML = \'<p>\' + buff + \'</p>\'' . "\n";
			}
		}
    ?>
  }else{
    document.getElementById("output").innerHTML = "Waiting ...";

    <?php
    echo "setTimeout('reloadPosts()', $refresh);\n";
    ?>
  }
}

<?php
echo "setTimeout('reloadPosts()', $refresh);\n"
?>

</script>

</body>
</html>
