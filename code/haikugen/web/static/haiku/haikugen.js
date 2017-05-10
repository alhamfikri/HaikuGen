$(function() {

	loadTemplates('haikugen-templates.html');
	
	// wire it up
	assert($('#topic-form')).submit(function(event) {
		event.preventDefault();
		var topic = assert($('#topic-input')).val();
		var topic2 = $('#topic-input2').val();
		makeHaiku(topic, topic2);	
		return false;
	});		

	var $apidoc = $('#api-doc');
	var h = $apidoc.html();
	h = h.replace(/_host_/g, window.location.host);
	$apidoc.html(h);
	
});

/**
 * 
 * @param topic {!string} A word or phrase to compose about.
 */
function makeHaiku(topic, topic2) {
	if ( ! topic) {
		return;
	}
	
	$('#haiku-output').text("Composing...");
	
	var url = '/haiku.json?topic='+escape(topic)+'&topic2='+(topic2? escape(topic2) : '');
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
	