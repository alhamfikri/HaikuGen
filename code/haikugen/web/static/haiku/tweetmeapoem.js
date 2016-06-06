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

/**
 * 
 * @param topic {!string} A word or phrase to compose about.
 */
function makeHaiku(username, topic) {
	
	$('#haiku-output').text("Composing...");
	
	var url = '/haiku.json?tweep='+escape(username)+'&topic='+escape(topic);
	$.get(url)
	.then(function(result) {
		console.log('Haiku',result);
		$('#haiku-output').html("");		
		var haikus = result.cargo;
		for(var i=0; i<3; i++) {
			var haiku = haikus[i];
			console.log(i, haiku);
			$('#haiku-output').append(templates.DisplayHaiku({text:haiku}));
		}
	});
}
	