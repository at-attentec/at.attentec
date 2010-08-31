//We want to redirect to /manual/ from /manual,
//otherwise pictures, styles and links don't work.
var href = location.href;
var end = "manual";
if (href.substr(href.length - end.length, end.length) == end) {
	location.href = href + '/';
}

//Fix QR-code for download
window.onload = function () {
	var pub_path = window.location.href.match("(.*)/manual/")[1];
	var imgs = document.getElementsByTagName('img');
	for (var i = imgs.length - 1; i >= 0; i--){
		var e = imgs[i];
		if (e.className.match('download_qr')) {
			e.src="http://chart.apis.google.com/chart?cht=qr&chs=150x150&chl="+pub_path+"/downloads/at.attentec.apk";
		};
	};
};