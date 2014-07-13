<?php
if(!isset($_REQUEST['js'])){
	echo '<form method="get">' . "<br>\n";
	echo 'Javascript URL: <input type="text" name="js" size="100">' . "<br>\n";
	echo '<input type="submit" value="Create">' . "<br>\n";
	echo '</form>' . "<br>\n";
}

$text = isset($_REQUEST['text']) ? $_REQUEST['text'] : "Bookmarklet";
?>

<a href="javascript:void(d=document);void(h=d.getElementsByTagName('head')[0]);void((s=d.createElement('script')).setAttribute('src','<?php echo $_REQUEST['js']; ?>'));void(h.appendChild(s));"><?php echo $text; ?></a>
