//Set DAP server
dap = "http://" + "<?php echo $_SERVER['SERVER_NAME']; ?>";
console.log(dap);

//Load CSS
var css = document.createElement('link');
css.rel = 'stylesheet';
css.type = 'text/css';
css.href = dap + '/bookmarklet/menu.css';
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
	$('a').each(function() {
		var link = this;
		var href = this.href;

		//Get supported outputs for this file
		$.getJSON(dap + '/bookmarklet/outputs.php?input=' + href.split('.').pop(), function(outputs) {
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
				var li = $('<li/>').appendTo(menu_list);
				var a = $('<a/>').text(outputs[i]).appendTo(li);
				a.attr('href', dap + ':8184/convert/' + outputs[i] + '/' + encodeURIComponent(href));
			});

			$(link).replaceWith(new_link);
		});
	});
};

function openMenu() {
	$(this).find('ul').css('visibility', 'visible');	
};
		
function closeMenu() {
	$(this).find('ul').css('visibility', 'hidden');	
};
