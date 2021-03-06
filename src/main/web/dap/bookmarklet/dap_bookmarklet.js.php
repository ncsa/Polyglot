//Set DAP server
protocol = 'http://';
dap = "<?php echo $_SERVER['SERVER_NAME']; ?>";
username = getCookie('username');
password = getCookie('password');

console.log(dap);
console.log(username + ':' + password);

//Load CSS
var css = document.createElement('link');
css.rel = 'stylesheet';
css.type = 'text/css';
css.href = protocol + dap + '/dap/bookmarklet/menu.css';
document.getElementsByTagName('head')[0].appendChild(css);

//Load JQuery
//if (!window.jQuery) {
	var jq = document.createElement('script');
	jq.type = 'text/javascript';
	jq.addEventListener('load', checkAuthorization);
	jq.src = "https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js";
	document.getElementsByTagName('head')[0].appendChild(jq);
//} else {
//	addMenuToLinks();
//}

//Check if authorization is needed for DAP before attempting to add menus
function checkAuthorization() {
	$.ajax({
   	url: protocol + dap + ':8184/alive',
		beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); },
		success: function() {
			addMenuToLinks();
		},
		error: function(jqXHR, textStatus, errorThrown) {
			console.log('Status: ' + jqXHR.status + ', ' + errorThrown);
			
			authorization = prompt("Authorization");
			strings = authorization.split(':');
			username = strings[0];
			password = strings[1];
			document.cookie = "username=" + username;
			document.cookie = "password=" + password;

			checkAuthorization();
		}
	});
}

//Process webpage once loaded
function addMenuToLinks() {
	//Add graphic
	addGraphic();
	
	//Add link back to self	
	var self = $('<a/>')
		.text('This file')
		.attr('href', window.location.href)
		.attr('id', 'self');
		
	$("body").append('<br>');
	$("body").append(self);

	//Proccess links
	$('a').each(function() {
		var link = this;
		var href = this.href;
		var input = href.split('.').pop();

		//Check for parameters
		if(input.indexOf('?') > -1){
			input = input.split('?')[0];
		}

		//Get supported outputs for this file
		if(input){
			console.log('Input: ' + input);

			//$.getJSON(protocol + dap + '/dap/bookmarklet/outputs.php?input=' + input, function(outputs) {
			$.ajax({url: protocol + dap + ':8184/inputs/' + input, beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); }}).done(function(outputs) {
				if(outputs) {
					outputs = outputs.split("\n");
					outputs = outputs.filter(function(v){return v!==''});
					//console.log(outputs);

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
						 	//.attr('href', 'http://' + dap + ':8184/convert/' + outputs[i] + '/' + encodeURIComponent(href))
							.attr('href', '#')
							.data('href', href)
							.data('output', outputs[i])
							.on('click', convert)
							.text(outputs[i])
							.appendTo(li);
					});

					$(link).replaceWith(new_link);
				}
			});
		}
	});
};

//Issue a conversion request
function convert() {
	console.log($(this).data('href') + ' -> ' + $(this).data('output'));

	//Set cursor to wait
	$('html,body').css('cursor', 'wait');
	$('a').each(function() {
		$(this).css('cursor', 'wait');
	});

	$.ajax({
    headers: {Accept: "text/plain"},
		url: protocol + dap + ':8184/convert/' + $(this).data('output') + '/' + encodeURIComponent($(this).data('href')),
		beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); }
	}).then(function(data) {
		redirect(data);
	});
}

//Redirect to the given url once it exists
function redirect(url) {
	//console.log('Redirecting: ' + url);

	$.ajax({
		//xhrFields: { withCredentials: true },
		//crossDomain: true,
		//type: 'HEAD',
   	url: url,
		beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); },
		success: function() {
			console.log('Redirected: ' + url);
			//window.location.href = url;
			window.location.href = addAuthentication(url);
		},
		error: function(jqXHR, textStatus, errorThrown) {
			console.log('Status: ' + jqXHR.status + ', ' + errorThrown);

			if(jqXHR.status == 0) {
				console.log('Redirected: ' + url);
				//window.location.href = url;
				window.location.href = addAuthentication(url);
			}else{
				setTimeout(function() {redirect(url);}, 1000);
			}
		}
	});
}

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');

	for(var i=0; i<ca.length; i++) {
		var c = ca[i];
		while(c.charAt(0)==' ') c = c.substring(1);
		if(c.indexOf(name) == 0) return c.substring(name.length, c.length);
	}

	return '';
} 

function addAuthentication(url) {
	if(username && password) {
		strings = url.split('//');
		url = strings[0] + '//' + username + ':' + password + '@' + strings[1];
	}

	return url;
}

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

//Brown Dog graphic
function addGraphic() {
	//Preload images
	//$.get(protocol + dap + '/dap/images/browndog-small-transparent.gif');
	//$.get(protocol + dap + '/dap/images/poweredby-transparent.gif');

	var graphic = $('<img>')
		.attr('src', protocol + dap + '/dap/images/browndog-small-transparent.gif')
		.attr('width', '25')
		.attr('id', 'graphic')
		.css('position', 'fixed')
		.css('left', '0px')
		.css('bottom', '25px');
	$("body").append(graphic);

	setTimeout(moveGraphicRight, 10);
}

function moveGraphicRight() {
	var graphic = document.getElementById('graphic');
	graphic.style.left = parseInt(graphic.style.left) + 25 + 'px';

	if(parseInt(graphic.style.left) < $(window).width() - 50) {
		setTimeout(moveGraphicRight, 10);
	} else {
		//graphic.remove();
		graphic.parentNode.removeChild(graphic);

		//Add powered by graphic
		graphic = $('<img>')
			//.attr('src', protocol + dap + '/dap/images/poweredby-transparent.gif')
			.attr('src', protocol + dap + '/dap/images/poweredby.gif')
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
