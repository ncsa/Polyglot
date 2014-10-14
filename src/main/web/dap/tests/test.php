<?php
$dap = isset($_REQUEST['dap']) ? $_REQUEST['dap'] : "";
$file = isset($_REQUEST['file']) ? $_REQUEST['file'] : "";
$output = isset($_REQUEST['output']) ? $_REQUEST['output'] : "";
$prefix = isset($_REQUEST['prefix']) ? $_REQUEST['prefix'] : "";
$run = isset($_REQUEST['run']) ? filter_var($_REQUEST['run'], FILTER_VALIDATE_BOOLEAN) : true;    //Set to false to return results from previous run
$mail = isset($_REQUEST['mail']) ? filter_var($_REQUEST['mail'], FILTER_VALIDATE_BOOLEAN) : false;
$temp_path = "tmp/";

$input_filename = basename(urldecode($file));
$output_filename = basename($input_filename, pathinfo($input_filename, PATHINFO_EXTENSION)) . $output;

if($prefix) {
	$output_file = $temp_path . $prefix . "_" . $output_filename;
} else {
	$output_file = $temp_path . $output_filename;
}

//Run through Polyglot
if($run) {
	$api_call = $dap . ":8184/convert/" . $output . "/" . urlencode($file);
	$command = "wget -O " . $output_file . " " . $api_call;

	exec($command);
}

//Check if non-empty output file was created
$success = false;

if(file_exists($output_file)) {
	if(filesize($output_file) > 0) {
		$success = true;
	}
}

if($success) {
	echo 1;
} else {
	if($mail) {
		$watchers = file("watchers.txt");

		foreach($watchers as $address) {
			$message = "Test-" . $prefix . " failed.  Output file of type \"" . $output . "\" was not created from:\n\n" . $file . "\n\n";
			$message .= "Report of last run can be seen here: \n\n http://" . $_SERVER['SERVER_NAME'] . "/dap/tests/tests.php?run=false&start=true\n";

			mail($address, "DAP Test Failed", $message);
		}
	}

	echo 0;
}
?>
