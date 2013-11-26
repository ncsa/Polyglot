<?php
$file = $_GET["file"];
?>

<html>
<head>
<script src="Three.js"></script>
<script src="plane.js"></script>
<script src="thingiview.js"></script>
<script>
	window.onload = function(){
		thingiurlbase = ".";
		thingiview = new Thingiview("viewer");
		thingiview.setObjectColor('#C0D8F0');
		thingiview.initScene();
		thingiview.loadOBJ("<?php echo $file; ?>");
	}
</script>
</head>
<body>
<div id="viewer" style="width:600px;height:400px"></div>
</body>
</html>
