function quoteClick (elem) {
		var offset = $(elem).offset();
		
		if (!($('#quoteoverlay').is(':visible'))) {
			$('#quoteoverlay')
				.css("display", "block")
				.css("top", offset.top);
		} 
		offset.top = getCloneTop ();
		var href = $(elem).attr('href');
		var segments = href.split("/");
		var id = segments[segments.length - 1];
		var div = cloneNode (id, offset.top);
		var cButton = $(div).prepend('<p class="closebutton">close</p>');
		$(div).find(".closebutton").click (function() {
			$('#backdrop').fadeOut();
			$('#quoteoverlay').css("display", "none")
			$('body').css("overflow", "visible");
			$(div).remove();
		});
		var cloneBottom = offset.top+$(div).outerHeight(true);
		
		var parentId = $(elem).parents("div").attr('id');
		$('#backdrop').fadeIn();
		$('html,body').animate({scrollTop: $("#"+parentId).offset().top},'slow', function() {
			//$('html,body').css("overflow", "hidden");
		  });	
		$('#backdrop').css("top", $(elem).parents("div").offset().top);
		return false;
	}
		
	function cloneNode (id, topOffset) {
		var div = $('div#' + id).clone(true, true)
		.attr('id', getCloneID (id))
		.attr('class','cloned')
		.css("top", topOffset + 20)
		.appendTo('#quoteoverlay');
		return div;
	}
	
	function getCloneID (id) {
		return id + '_cloned';
	}
	
	function getCloneTop () {
		var top = 0;
		var lastClone = ($('#quoteoverlay div.cloned').last());
		if (null != lastClone.offset()) {
			top = $(lastClone).position().top + 60;
		} else {
			top = 20;
		}
		return top;
	}
	
	function goToByScroll(id){     			
		$('html,body').animate({scrollTop: $("#"+id).offset().top},'slow');		
	}