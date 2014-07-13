<?php
header('Content-type: application/json');
header('Access-Control-Allow-Origin: *');
$input = isset($_REQUEST['input']) ? $_REQUEST['input'] : "";
$outputs = file_get_contents('http://localhost:8184/inputs/' . $input);
echo json_encode(array_filter(explode("\n", $outputs)));
?>
