//Set DAP server
var protocol = 'http://';
var dap = "<?php echo $_SERVER['SERVER_NAME']; ?>";
var username = getCookie('username');
var password = getCookie('password');

console.log(dap);
console.log(username + ':' + password);

//Load JQuery
//if(!window.jQuery) {
	var jq = document.createElement('script');
	jq.type = 'text/javascript';
	jq.addEventListener('load', checkAuthorization);
	jq.src = "https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js";
	document.getElementsByTagName('head')[0].appendChild(jq);
//} else {
//	checkAuthorization();
//}

//Check if authorization is needed for DAP before attempting to add menus
function checkAuthorization() {
  $.ajax({
    url: protocol + dap + ':8184/alive',
    beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); },
    success: function() {
      processLinks();
    },
    error: function(jqXHR, textStatus, errorThrown) {
      console.log('Status: ' + jqXHR.status + ', ' + errorThrown);
			
			if(jqXHR.status == 0) {
				processLinks();
			}else{
      	authorization = prompt("Authorization");
      	strings = authorization.split(':');
      	username = strings[0];
      	password = strings[1];
      	document.cookie = "username=" + username;
      	document.cookie = "password=" + password;

      	checkAuthorization();
			}
    }
  });
}

//Process webpage once loaded
function processLinks() {
	addGraphic();

	$("a[dap]").each(function() {
		$(this).data('href', this.href);
		$(this).data('output', $(this).attr('dap'));
		$(this).attr('href', '#');
		$(this).on('click', convert)
	});
	
	$("img[dap]").each(function() {
		console.log(this.src + ' -> ' + $(this).attr('dap'));
		var img = this;

		$.ajax({
    	headers: {Accept: "text/plain"},
			url: protocol + dap + ':8184/convert/' + $(this).attr('dap') + '/' + encodeURIComponent(this.src),
			beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); }
		}).then(function(data) {
  		$(img).css('cursor', 'wait');		//Set cursor to wait
			reload(img, data);
		});
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
	$.ajax({
		//type: 'HEAD',
   	url: url,
		beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); },
		success: function() {
			console.log('Redirected: ' + url);
			window.location.href = addAuthentication(url);
		},
		error: function(jqXHR, textStatus, errorThrown) {
			console.log('Status: ' + jqXHR.status + ', ' + errorThrown);

			if(jqXHR.status == 0) {
				console.log('Redirected: ' + url);
				window.location.href = addAuthentication(url);
			}else{
				setTimeout(function() {redirect(url);}, 1000);
      }
		}
	});
}

//Redirect the image once it source exists
function reload(img, url) {
	//console.log(img.src + ', ' + url);

	$.ajax({
		//type: 'HEAD',
   	url: url,
		beforeSend: function(req) { req.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password)); },
		success: function() {
			console.log('Reloading: ' + url);
  		$(img).css('cursor', 'default');		//Set cursor to default
			img.src = addAuthentication(url);
		},
		error: function(jqXHR, textStatus, errorThrown) {
			console.log('Status: ' + jqXHR.status + ', ' + errorThrown);

			if(jqXHR.status == 0) {
				console.log('Reloading: ' + url);
  			$(img).css('cursor', 'default');		//Set cursor to default
				img.src = addAuthentication(url);
			}else{
				setTimeout(function() {reload(img, url);}, 1000);
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

//Brown Dog graphic
function addGraphic() {
	//Preload images
	$.get(protocol + dap + '/dap/images/browndog-small-transparent.gif');
	$.get(protocol + dap + '/dap/images/poweredby-transparent.gif');

	var graphic = $('<img>')
		.attr('src', protocol + dap + '/dap/images/browndog-small-transparent.gif')
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
			.attr('src', protocol + dap + '/dap/images/poweredby-transparent.gif')
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
