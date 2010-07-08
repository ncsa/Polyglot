<!--
Automatically call a php script every number of seconds so as to refresh 
displayed outputs.
-->

<html>
<body link="blue" vlink="blue">

<?php
//Set PHP variables
$file = $_GET["auto_refresh_file"];
$refresh = $_GET["refresh"];
if($refresh == null) $refresh = 1000;
$tmp = explode("/", $file);
$name = $tmp[sizeof($tmp)-1];
$tmp = explode(".", $name);
$ext = $tmp[sizeof($tmp)-1];

if($ext == "php"){	//Call a php script and pass through it's parameters
  $keys = array_keys($_GET);
  $count = 0;

  for($i=0; $i<sizeof($keys); $i++){
    if($keys[$i] != "auto_refresh_file"){
      if($count == 0){
        $file = $file . "?";
      }else{
        $file = $file . "&";
      }

      $file = $file . $keys[$i] . "=" . $_GET[$keys[$i]];
      $count++;
    }
  }
}
?>

<center>
<div id="output" align="left"></div>
</center>

<script language="JavaScript">
var SCROLL_BAR_SET = false;
var scroll_bottom = -1;

/*
//Get current directory
var path = window.location.href;
var tmp = path.split("/");
delete tmp[(tmp.length-1)];
var path = tmp.join("/");
*/

function AJAXCall(file)
{
  xmlHttp = new XMLHttpRequest();
  if(xmlHttp == null){ alert("Your browser does not support AJAX!"); return; }
  xmlHttp.open("GET", file, false);
  xmlHttp.send(null);
  buff = xmlHttp.responseText;

  return buff;
}

function reloadPosts(){
  <?php
  if($ext == "jpg"){
    echo 'document.getElementById("output").innerHTML = \'<img src="' . $file . '?\' + (new Date()).getTime() + \'">\';' . "\n";
  }else{
    echo 'var tmp = "' . $file . '";' . "\n";
    echo 'var buff = AJAXCall(tmp);';

    if($ext == "txt" || $name == "log"){
      echo 'document.getElementById("output").innerHTML = \'<pre>\' + buff + \'</pre>\'' . "\n";
    }else{
      echo 'document.getElementById("output").innerHTML = \'<p>\' + buff + \'</p>\'' . "\n";
    }
  }
  ?>

  if(!SCROLL_BAR_SET){
    window.scroll(0,1000000);
    SCROLL_BAR_SET = true;
    scroll_bottom = window.pageYOffset;
  }else{
    var scroll_pos = window.pageYOffset;

    if(scroll_pos >= scroll_bottom){
      window.scroll(0,1000000);
      scroll_bottom = scroll_pos;
    }
  }

  <?php
  echo "setTimeout('reloadPosts()', $refresh);"
  ?>
}

<?php
echo "setTimeout('reloadPosts()', $refresh);"
?>

</script>

</body>
</html>
