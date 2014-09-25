<?php
header('Content-type: application/json');
header('Access-Control-Allow-Origin: *');
$input = isset($_REQUEST['input']) ? $_REQUEST['input'] : "";

//$outputs = file_get_contents('http://localhost:8184/inputs/' . $input);

//$outputs = url_get_contents('http://localhost:8184/inputs/' . $input);

$outputs = shell_exec('curl http://localhost:8184/inputs/' . $input );

echo json_encode(array_filter(explode("\n", $outputs)));

/*
function url_get_contents ($Url) {
    if (!function_exists('curl_init')){ 
        die('CURL is not installed!');
    }
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $Url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $output = curl_exec($ch);
    curl_close($ch);
    return $output;
}
*/
?>
