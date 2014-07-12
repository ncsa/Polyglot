//Set DAP server
var dap = "http://" + "<?php echo $_SERVER['SERVER_NAME']; ?>" + ":8184/convert/";

//Load JQuery
if (!window.jQuery) {
	var jq = document.createElement('script');
	jq.type = 'text/javascript';
	jq.src = "http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js";
	document.getElementsByTagName('head')[0].appendChild(jq);
}

//Process webpage once loaded
window.onload = function() {
	$("a[dap]").each(function() {
		this.href = dap + $(this).attr('dap') + "/" + encodeURIComponent(this.href);
	});
};
