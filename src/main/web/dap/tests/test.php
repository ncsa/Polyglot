<?php
$dap = isset($_REQUEST['dap']) ? $_REQUEST['dap'] : "";
$file = isset($_REQUEST['file']) ? $_REQUEST['file'] : "";
$output = isset($_REQUEST['output']) ? $_REQUEST['output'] : "";
$prefix = isset($_REQUEST['prefix']) ? $_REQUEST['prefix'] . "_" : "";
$temp_path = "tmp/";

$api_call = $dap . ":8184/convert/" . $output . "/" . urlencode($file);
$input_filename = basename(urldecode($file));
$output_filename = basename($input_filename, pathinfo($input_filename, PATHINFO_EXTENSION)) . $output;
$output_file = $temp_path . $prefix . $output_filename;
$command = "wget -O " . $output_file . " " . $api_call;

exec($command);

if(file_exists($output_file)) {
	echo filesize($output_file);
} else {
	echo 0;
}
?>
