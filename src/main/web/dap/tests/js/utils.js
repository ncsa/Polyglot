//Make calls to server
function AJAXCall(file)
{
	xmlHttp = new XMLHttpRequest();
	if(xmlHttp == null){alert("Your browser does not support AJAX!"); return;}
	xmlHttp.open("GET", file, false);
	xmlHttp.send(null);
	buff = xmlHttp.responseText;

	return buff;
}
