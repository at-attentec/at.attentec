/***
 * Copyright (c) 2010 Attentec AB, http://www.attentec.se
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function details(obj) {
	if ($(obj).html() == '(+)') { //Show
		var path = $(obj).parents('tr').children('td').children('a.show_link').attr('href');
		$.get(path, {}, null, 'script');
		$(obj).html("(-)");
	}
	else { //hide
		var id = $(obj).parents('tr').attr('id').substr("show_".length);
		$('#details_'+id).remove();
		$(obj).html("(+)");
	};
}

$(document).ready(function() {
	$.ajaxSetup({
	  beforeSend: function (xhr) { xhr.setRequestHeader("Accept", "text/javascript"); }
 	});
	$('#userlist tr td:first-child').prepend('<a class="details_link" onclick="details(this);">(+)</a> ');
});
