<!--
Display contents of a directory as a mosaic of content.
-->

<html>
<body>
<center>

<?php
include "utils.php";

$dir = $_GET["dir"];
$files = scandir($dir);

//Read in content types
$fp = fopen("../var/content_types.txt", "r");
$map = NULL;

while(!feof($fp)){
	$type = chop(fgets($fp));						//Read in content type

	$line = chop(fgets($fp));						//Read in formats list
	$extensions = explode(" ", $line);

	for($i=0; $i<sizeof($extensions); $i++){
		$map[$extensions[$i]] = $type;
	}

	if(strlen($line) > 0) fgets($fp);		//Read in empty line
}

//Build list of valid files
$valid_files = array();
$types = NULL;

for($i=0; $i<sizeof($files); $i++){
  if($files[$i] != "." && $files[$i] != ".."){
		$dot = strrpos($files[$i],'.');

    if($dot !== false){
      $ext = substr($files[$i], $dot+1);
			$valid_files[sizeof($valid_files)] = $files[$i];
			$types[$files[$i]] = $map[$ext];
    }
  }
}

//$types = array_unique($types);	//Buggy!
$valid_files = array_keys(array_count_values($valid_files));

for($i=0; $i<sizeof($valid_files); $i++){
	$dot = strrpos($valid_files[$i],'.');
	$name = substr($valid_files[$i], 0, $dot);
	$truncated_name = $name;

	if(strlen($truncated_name) > 8){
		$truncated_name = substr($truncated_name, 0, 7) . "*";
	}

	$file = "";
	$file_truncated = "";

  if($types[$valid_files[$i]] == '3d'){
		$file = $name . ".obj";
		$file_truncated = $truncated_name . ".obj";
	}else if($types[$valid_files[$i]] == 'image'){
		$file = $name . ".jpg";
		$file_truncated = $truncated_name . ".jpg";
	}else if($types[$valid_files[$i]] == 'document'){
		$file = $name . ".txt";
		$file_truncated = $truncated_name . ".txt";
	}else if($types[$valid_files[$i]] == 'audio'){
		$file = $name . ".wav";
		$file_truncated = $truncated_name . ".wav";
	}

  $size = bytes_hr(filesize("$dir/$file"));

  echo '<iframe name="f' . $i . '" id="f' . $i . '" width="224" height="252" frameborder="0"></iframe>' . "\n";
  echo '<script>' . "\n";

  if(false){    //Option-1: Use the given iframe
    echo 'document.getElementById(\'f' . $i . '\').src = "file_viewer.php?file=' . $dir . '/' . $file . '&width=200&height=200&refresh=100";' . "\n";
    echo "</script>\n";
    echo '<a href="' . $dir . '/' . $file . '" target="_top">' . $file . '</a> (' . $size . ')' . "\n";
  }else{        //Option-2: Split the given iframe so that text can be placed under applet
    echo 'var iframe = document.getElementById(\'f' . $i . '\');' . "\n\n";
    ?>
    iframe.doc = null;
    if(iframe.contentDocument)
      iframe.doc = iframe.contentDocument;
    else if(iframe.contentWindow)
      iframe.doc = iframe.contentWindow.document;
    else if(iframe.document)
      iframe.doc = iframe.document;

    iframe.doc.open();
    iframe.doc.close();
    var frm = iframe.doc.createElement('iframe');
    frm.style.border = "0";
    frm.style.width = "216";
    frm.style.height = "216";
    var div = iframe.doc.createElement('div');
    <?php
    echo 'frm.src = "file_viewer.php?file=' . $dir . '/' . $file . '&width=193&height=193&refresh=100";' . "\n";
    //echo 'div.innerHTML = "<center><a href=\"' . $dir . '/' . $file . '\" target=\"_top\">' . $file_truncated . '</a> (' . $size . ')</center>"' . "\n";
    echo 'div.innerHTML = "<center><a href=\"' . $dir . '/' . $file . '\" target=\"_top\">' . $file_truncated . '</a></center>"' . "\n";
    ?>
    iframe.doc.body.appendChild(frm);
    iframe.doc.body.appendChild(div);
    </script>
    <?php
  }
}
?>

</center>
</body>
</html>
