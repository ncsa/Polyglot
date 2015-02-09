<?php
$js = isset($_REQUEST['js']) ? $_REQUEST['js'] : "";
$title = isset($_REQUEST['title']) ? $_REQUEST['title'] : "Bookmarklet";
$favicon = isset($_REQUEST['favicon']) ? $_REQUEST['favicon'] : "";
$image = isset($_REQUEST['image']) ? $_REQUEST['image'] : "";
$description = isset($_REQUEST['description']) ? $_REQUEST['description'] : "";
$modify = isset($_REQUEST['modify']) ? $_REQUEST['modify'] : "";
?>
<html>
<head>
<title><?php echo $title; ?></title>
<link rel="shortcut icon" href="<?php echo $favicon; ?>" >
</head>

<body>
<?php
if (!$js || $modify) {
	echo '<form method="get">' . "<br>\n";
	echo 'Javascript URL: <input type="text" name="js" size="100" value="' . $js . "\"><br>\n";
	echo 'Title (optional): <input type="text" name="title" size="100" value="' . $title . "\"><br>\n";
	echo 'Favicon (optional): <input type="text" name="favicon" size="100" value="' . $favicon. "\"><br>\n";
	echo 'Image (optional): <input type="text" name="image" size="100" value="' . $image . "\"><br>\n";
	echo 'Description (optional): <input type="text" name="description" size="100" value="' . $description . "\"><br>\n";
	echo '<input type="submit" value="Create">' . "<br>\n";
	echo '</form>' . "<br>\n";
} else {
	if (!$image) {
	?>
<a href="javascript:void(d=document);void(h=d.getElementsByTagName('head')[0]);void((s=d.createElement('script')).setAttribute('src','<?php echo $js; ?>'));void(h.appendChild(s));"><?php echo $title; ?></a>
	<?php
	} else {
	?>
<center>
<h1 style="font-family:arial;color:gray;"><?php echo $title; ?></h1>
<h2 style="font-family:arial;color:lightgray;">Drag icon to bookmark toolbar to install</h2>
<a href="javascript:void(d=document);void(h=d.getElementsByTagName('head')[0]);void((s=d.createElement('script')).setAttribute('src','<?php echo $js; ?>'));void(h.appendChild(s));"><img src="<?php echo $image; ?>" width="200" alt="<?php echo $title; ?>"></a>
<h3 style="font-family:arial;"><?php echo $description; ?></h3>
</center>
	<?php
	}
}
?>
</body>
</html>
