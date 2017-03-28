

function markupContextHelp() {
    $('.context-help').each(function(index, element) {
        // grab each element with context-help class
        var $element = $(element);

        // get the label and linkify
        var $label = $element.find(".context-help-label").first();
        $label.attr("data-icon", "?");
        $label.addClass("clickable");
        $label.click(function() {
            toggleContextHelp($label);
            return false;
        });

        // get the help content
        var $content = $element.find(".context-help-content").first();
        // style and wrap it
        $content.prepend("<div style='text-align:right;'>" +
                                 "<span onclick='toggleContextHelp(this); return false;' class='icon icon-functional clickable' data-icon='x'>" +
                                 "</span>" +
                                 "</div>");
        $content.wrap("<div class='context-help-wrapper'></div>");
        $content.show();
    });
}

function toggleContextHelp(element) {
    // run the effect
    var $parent = $(element).parents(".context-help").first();
    var $content = $parent.find(".context-help-wrapper").first();
    $content.toggle();
    return false;
}


function populateExamples() {
    $('#identifiers').val("EFO:0001360\nDOID:162\nOMIM:180200\nMESH:D009202\nUBERON_0002107\nHP_0005978");
}

function exportData(format) {

    console.log("fomart " + format)
    var filterForm = $('#filter-form');
    if (filterForm) {
        filterForm.attr('action', 'api/search?format='+format);
        filterForm.submit();
        filterForm.attr('action', 'search');
    }

}