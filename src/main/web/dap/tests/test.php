<?php
function my_http_response_code($url)
{
	$headers = get_headers($url);
	return substr($headers[0], 9, 3);
}

$dap = isset($_REQUEST['dap']) ? $_REQUEST['dap'] : "";
$file = isset($_REQUEST['file']) ? $_REQUEST['file'] : "";
$output = isset($_REQUEST['output']) ? $_REQUEST['output'] : "";
$prefix = isset($_REQUEST['prefix']) ? $_REQUEST['prefix'] : "";
$run = isset($_REQUEST['run']) ? filter_var($_REQUEST['run'], FILTER_VALIDATE_BOOLEAN) : true;    //Set to false to return results from previous run
$mail = isset($_REQUEST['mail']) ? filter_var($_REQUEST['mail'], FILTER_VALIDATE_BOOLEAN) : false;
$temp_path = "tmp/";
$wait = 60;

$input_filename = basename(urldecode($file));
$output_filename = basename($input_filename, pathinfo($input_filename, PATHINFO_EXTENSION)) . $output;

if($prefix) {
	$output_file = $temp_path . $prefix . "_" . $output_filename;
} else {
	$output_file = $temp_path . $output_filename;
}

//Run through Polyglot
if($run) {
	$opts = array();
	$username = "";
	$password = "";

	//Add authentication if required
	if(strpos($dap,'@') !== false) {
		$parts = explode('@', $dap);
		$dap = $parts[1];
		$parts = explode(':', $parts[0]);
		$username = $parts[0];
		$password = $parts[1];

		//Set defaults for get_headers function
		$opts = array('http'=>array('method'=>"HEAD", 'header'=>"Accept:text/plain\r\n" . "Authorization: Basic " . base64_encode("$username:$password") . "\r\n"));
		stream_context_set_default($opts);

		//Set options for api call
		$opts = array('http'=>array('method'=>"GET", 'header'=>"Accept:text/plain\r\n" . "Authorization: Basic " . base64_encode("$username:$password") . "\r\n"));
	} else {
		$opts = array('http'=>array('method'=>"GET", 'header'=>"Accept:text/plain\r\n"));
	}

	$api_call = "http://" . $dap . ":8184/convert/" . $output . "/" . urlencode($file);
	$context = stream_context_create($opts);
	$result = file_get_contents($api_call, false, $context);
	//error_log("Result: " . $result);

	if($result){
		while($wait > 0 && my_http_response_code($result) == "404") {
			sleep(1);
			$wait--;
		}

		if(my_http_response_code($result) != "404") {
			$command = "";

			if($username && $password) {
				$command = "wget --user=" . $username . " --password=" . $password . " -O " . $output_file . " " . $result;
			} else {
				$command = "wget -O " . $output_file . " " . $result;
			}

			exec($command);
		}
	}
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
