$(function() {

	loadTemplates('haikugen-templates.html');
	
	// wire it up
	assert($('#topic-form')).submit(function(event) {
		event.preventDefault();
		var topic = assert($('#topic-input')).val();
		makeHaiku(topic);	
		return false;
	});		

});

/**
 * 
 * @param topic {!string} A word or phrase to compose about.
 */
function makeHaiku(topic) {
	if ( ! topic) {
		return;
	}
	
	$('#haiku-output').text("Composing...");
	
	$.get('/haiku.json?topic='+escape(topic))
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
	