function createChannel(token) {
	channel = new goog.appengine.Channel(token);
	socket = channel.open();
	socket.onopen = function() {
		console.log("Channel API Opened");
	};
	socket.onerror = function(obj) {
		console.log("Channel API Error:");
		console.log(obj);
	};
	socket.onclose = function() {
		console.log("Channel Api Closed");
	};
	socket.onmessage = function(obj) {
		var sdata = obj.data.split(":");
		channelMessage(sdata.shift(), sdata);
	};
}      
