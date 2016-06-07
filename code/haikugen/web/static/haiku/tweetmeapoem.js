$(function() {

	loadTemplates('haikugen-templates.html');
	
	// wire it up
	assert($('#topic-form')).submit(function(event) {
		event.preventDefault();
		var topic = $('#topic-input').val();
		var username = assert($('#username')).val();
		makeHaiku(username, topic);	
		return false;
	});		
	
});

function pollPoetry() {
	var url = '/haiku.json?action=recent';
	$.get(url)
	.then(function(result) {
//		console.log('Haiku-recent',result);	
		var haikus = result.cargo;
		for(var i=haikus.length-1; i>=0; i--) {
			var haiku = haikus[i];
//			console.log("poll "+i, haiku);		
			if ($('#'+haiku.id).length===0) {
				console.log("poll "+i, haiku);
				$('#haiku-output').prepend(templates.DisplayHaiku(haiku));
			} else {
//				console.log("old news", haiku);	
			}
		}
	})
	.fail(function(a,b,c){
		console.warn(a,b,c);
		$msg = $("<div class='alert alert-warning' role='alert'>Infinite Contemplation Error: Haiku Bot is overloaded just now - please try again in a minute.</div>");
		$('#msgbox').append($msg);
		$msg.delay(5000).fadeOut();
	});
}

setInterval(pollPoetry, 5000);

/**
 * 
 * @param topic {!string} A word or phrase to compose about.
 */
function makeHaiku(username, topic) {	
	var url = '/haiku.json?tweep='+escape(username)+'&topic='+escape(topic);
	$.get(url)
	.then(function(result) {
		// let the poll do it
	})
	.fail(function(a,b,c){
		console.warn(a,b,c);
		$msg = $("<div class='alert alert-warning' role='alert'>Infinite Contemplation Error: Haiku Bot is overloaded just now - please try again in a minute.</div>");
		$('#msgbox').append($msg);
		$msg.delay(2000).fadeOut();
	});
	
	$msg = $("<div class='alert alert-success' role='alert'>Fetching tweets and composing poem...</div>");
	$('#msgbox').append($msg);
	$msg.delay(2000).fadeOut();	
}
	