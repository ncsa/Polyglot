<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Brown Dog Resources Status</title>

    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
      <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>

  <body>
		<br>
		<div class="container">
			<div class="jumbotron">
    		<h1>DAP Tests</h1>
				<div id="failures" style="color:#999999;font-style:italic;font-size:90%;"></div>
			</div>
				
			<input type="button" class="btn btn-lg btn-block btn-primary" value="Run Tests" onclick="run_tests()">
			<!--
			<div class="progress">
  			<div id="progress" class="progress-bar" role="progressbar" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100" style="width: 50%">50%</div>
			</div>
			-->

			<table class="table table-bordered table-hover">
			<tr><th>#</th><th>Input</th><th>Output</th><th></th></tr>
		
			<?php
			$dap = "http://" . $_SERVER['SERVER_NAME'];
			$lines = file('tests.txt', FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
			$json = array();
			$count = 0;		//Row ID and unique prefix for output file

			foreach($lines as $line) {
				if($line[0] != '#') {
					$parts = explode(' ', $line);
					$outputs = explode(',', $parts[1]);
					$json[$parts[0]] = $outputs;

					foreach($outputs as $output) {
						$output_filename = basename($parts[0], pathinfo($parts[0], PATHINFO_EXTENSION)) . $output;

						echo "<tr id=\"" . ++$count . "\">";
						echo "<td>" . $count . "</td>";
						echo "<td><a href=\"" . $parts[0] . "\">" . $parts[0] . "</a></td>";
						echo "<td><a href=\"tmp/" . $count . "_" . $output_filename . "\">" . $output_filename . "</a></td>";
						echo "<td align=\"center\"><input type=\"button\" class=\"btn btn-xs btn-primary\" value=\"Run\" onclick=\"test(" . $count . ",'" . $parts[0] . "','" . $output . "')\"></td>";
						echo "</tr>\n";
					}
				}
			}
			?>

			</table>
		</div>

    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
    <script src="js/utils.js"></script>

		<script>
			<?php if(isset($_REQUEST['run'])) echo "run_tests();\n"; ?>

			function run_tests() {
				var tasks = $.parseJSON('<?php echo json_encode($json); ?>');
				var count = 0;	//Row ID and unique prefix for output file
				var successes = 0;
				var total = <?php echo $count; ?>;

				$.each(tasks, function(task) {
					$.each(tasks[task], function(i) {
						successes += test(++count, task, tasks[task][i]);
					});
				});
				
				//Update progress
				document.getElementById('failures').innerHTML = 'Failures: ' + (total - successes);
			}

			function test(id, file, output) {
				var row = document.getElementById(id.toString());
				$(row).addClass('info');

				var url = 'test.php?dap=' + encodeURIComponent('<?php echo $dap; ?>') + '&file=' + encodeURIComponent(file) + '&output=' + output + '&prefix=' + id;
				var size = AJAXCall(url);

				if(size > 0) {
					$(row).attr('class', 'success');
					return 1;
				} else {
					$(row).attr('class', 'danger');
					return 0;
				}
			}
		</script>
  </body>
</html>
