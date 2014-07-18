//Set DAP server
var dap = "http://" + "<?php echo $_SERVER['SERVER_NAME']; ?>";

//Load JQuery
if (!window.jQuery) {
	var jq = document.createElement('script');
	jq.type = 'text/javascript';
	jq.src = "http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js";
	document.getElementsByTagName('head')[0].appendChild(jq);
}

//Process webpage once loaded
window.onload = function() {
	addGraphic();

	$("a[dap]").each(function() {
		this.href = dap + ":8184/convert/" + $(this).attr('dap') + "/" + encodeURIComponent(this.href);
	});
};

//Brown Dog graphic
function addGraphic() {
	//Preload images
	$.get(dap + '/dap/images/browndog-small-transparent.gif');
	$.get(dap + '/dap/images/poweredby-transparent.gif');

	var graphic = $('<img>')
		.attr('src', dap + '/dap/images/browndog-small-transparent.gif')
		.attr('width', '25')
		.attr('id', 'graphic')
		.css('position', 'absolute')
		.css('left', '0px')
		.css('bottom', '25px')
	$("body").append(graphic);

	setTimeout(moveGraphicRight, 10);
}

function moveGraphicRight() {
	var graphic = document.getElementById('graphic');
	graphic.style.left = parseInt(graphic.style.left) + 25 + 'px';

	if(parseInt(graphic.style.left) < $(window).width() - 50) {
		setTimeout(moveGraphicRight, 10);
	} else {
		graphic.remove();

		//Add powered by graphic
		graphic = $('<img>')
			.attr('src', dap + '/dap/images/poweredby-transparent.gif')
			.attr('width', '100');

		var link = $('<a/>')
			.attr('href', 'http://browndog.ncsa.illinois.edu')
			.attr('id', 'poweredby')
			.css('position', 'fixed')
			.css('right', '10px')
			.css('bottom', '10px')
			.append(graphic);

		$("body").append(link);
	}
}
