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
		//this.href = dap + ":8184/convert/" + $(this).attr('dap') + "/" + encodeURIComponent(this.href);
		$(this).data('href', this.href);
		$(this).data('output', $(this).attr('dap'));
		$(this).attr('href', '#');
		$(this).on('click', convert)
	});
	
	$("img[dap]").each(function() {
		//this.src = dap + ":8184/convert/" + $(this).attr('dap') + "/" + encodeURIComponent(this.src);
	
		console.log(this.src + ' -> ' + $(this).attr('dap'));
		var img = this;

		$.ajax({
    	headers: {Accept: "text/plain"},
			url: dap + ':8184/convert/' + $(this).attr('dap') + '/' + encodeURIComponent(this.src)
		}).then(function(data) {
			reload(img, data);
		});
	});
};

//Issue a conversion request
function convert() {
	console.log($(this).data('href') + ' -> ' + $(this).data('output'));

	$.ajax({
    headers: {Accept: "text/plain"},
		url: dap + ':8184/convert/' + $(this).data('output') + '/' + encodeURIComponent($(this).data('href'))
	}).then(function(data) {
		redirect(data);
	});
}

//Redirect to the given url once it exists
function redirect(url) {
	$.ajax({
		type: 'HEAD',
   	url: url,
		success: function() {
			window.location.href = url;
		},
		error: function() {
			setTimeout(function() {redirect(url);}, 1000);
		}
	});
}

//Redirect the image once it source exists
function reload(img, url) {
	//console.log(img.src + ', ' + url);

	$.ajax({
		type: 'HEAD',
   	url: url,
		success: function() {
			img.src = url;
		},
		error: function() {
			setTimeout(function() {reload(img, url);}, 1000);
		}
	});
}

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
