//Set DAP server
dap = "http://" + "<?php echo $_SERVER['SERVER_NAME']; ?>";
console.log(dap);

//Load CSS
var css = document.createElement('link');
css.rel = 'stylesheet';
css.type = 'text/css';
css.href = dap + '/dap/bookmarklet/menu.css';
document.getElementsByTagName('head')[0].appendChild(css);

//Load JQuery
if (!window.jQuery) {
	var jq = document.createElement('script');
	jq.type = 'text/javascript';
	jq.addEventListener('load', addMenuToLinks);
	jq.src = "http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js";
	document.getElementsByTagName('head')[0].appendChild(jq);
} else {
	addMenuToLinks();
}

//Process webpage once loaded
function addMenuToLinks() {
	//Add graphic
	addGraphic();

	//Proccess links
	$('a').each(function() {
		var link = this;
		var href = this.href;

		//Get supported outputs for this file
		$.getJSON(dap + '/dap/bookmarklet/outputs.php?input=' + href.split('.').pop(), function(outputs) {
			console.log(outputs);

			//Replace link with a menu
			var new_link = $('<ul/>').addClass('menu');
			var new_link_item = $('<li/>')
				.bind('mouseover', openMenu)
				.bind('mouseout', closeMenu)
				.appendTo(new_link);
			$(link).clone().appendTo(new_link_item);

			var menu = $('<li/>').appendTo(new_link_item);
			var menu_list = $('<ul/>').appendTo(menu);

			$.each(outputs, function(i) {
				var li = $('<li/>')
					.on('DOMMouseScroll', scrollMenu)
					.appendTo(menu_list);

				var a = $('<a/>')
					.attr('href', dap + ':8184/convert/' + outputs[i] + '/' + encodeURIComponent(href))
					.text(outputs[i])
					.appendTo(li);
			});

			$(link).replaceWith(new_link);
		});
	});
};

function openMenu() {
	$(this).find('ul').css('visibility', 'visible');	
}
		
function closeMenu() {
	$(this).find('ul').css('visibility', 'hidden');	
}

function scrollMenu(event) {
	if(event.originalEvent.detail > 0) {
		$(this).siblings().first().appendTo(this.parentNode);
	} else {
		$(this).siblings().last().prependTo(this.parentNode);
	}
	
	event.originalEvent.preventDefault();
}

function addGraphic() {
	var graphic = $('<img>')
		.attr('src', dap + '/dap/bookmarklet/images/browndog-small.gif')
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
			.attr('src', dap + '/dap/bookmarklet/images/poweredby.gif')
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
