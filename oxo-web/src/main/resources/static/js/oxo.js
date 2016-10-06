$(document).ready(function() {
    renderSearchResults();
});

/**
 * Find all elements on page with attribute data-get-mapping and append a div showing the mappings for that value
 * todo add additional filter options like data-source, data-target and data-distance to prodide additional filters
 *
 */
function renderSearchResults() {

    if ($( "div[data-mapping-ids]" ).length)

    $( "div[data-mapping-ids]" ).each(function() {

        var id = $(this).data("mapping-ids");
        var targets = $(this).data("mapping-targets") ? $(this).data("mapping-targets").split(",") : undefined;
        var sources = $(this).data("mapping-sources") ? $(this).data("mapping-sources").split(",") : undefined;
        var distance = $(this).data("mapping-distance") ? $(this).data("mapping-distance") : undefined;

        var requestData = {

            ids : id,
            mappingTarget: targets,
            mappingSource :  sources,
            distance: distance
        };

        $.ajax({
            url:"/api/search",
            dataType: 'json',
            method: 'GET',
            contentType: "application/json; charset=utf-8",
            data : requestData,
            traditional: true,
            context: this,
            success:function(data) {


                var res = data._embedded.searchResults[0].mappingResponseList;
                var requestId =   data._embedded.searchResults[0].curie;
                var requestLabel  =   data._embedded.searchResults[0].label;

                var table = $("<table style='width:100%;margin: 0;border: none;'/>");
                var headerRow = $("<tr/>")
                headerRow.append($("<th style='width:120px;'>Datasource</th><th style='width:250px;'>Id</th><th style='width:150px;'>Mapping source</th><th style='width:150px;'>Distance</th>"))
                table.append(headerRow);


                for (var x=0;x<res.length;x++) {
                    var tableRow = $("<tr/>")
                    var mapping = res[x];
                    var targetSource = res[x].targetPrefixes[0];

                    _mappingTargetFound(targetSource);

                    var targetLink = $('<a class="nounderline" style="border-bottom-width: 0px;"></a>').attr('href', '/datasources/' + targetSource);
                    var termLink = $('<a class="nounderline" style="border-bottom-width: 0px;"></a>').attr('href', '/terms/' + mapping.curie);

                    var targetSpan =  $('<span class="ontology-source"></span>').text(targetSource)
                    var curie =  $('<span class="term-source"></span>').text(mapping.curie)

                    var datasourceCell  =  $("<td/>");
                    targetLink.append(targetSpan);
                    datasourceCell.append(targetLink);
                    tableRow.append(datasourceCell);


                    var term =  $("<td/>");
                    termLink.append(curie);
                    term.append(termLink);

                    if (mapping.label != undefined) {
                        term.append(" (" + mapping.label + ")");
                    }

                    tableRow.append(term);
                    var sourceName =    $("<td/>");
                    $.each(mapping.sourcePrefixes, function (index, value){
                        var sourceLink = $('<a class="nounderline" style="border-bottom-width: 0px;"></a>').attr('href', '/datasources/' + value);

                        _mappingSourceFound(value)
                        sourceLink.append($('<span class=\"ontology-source\"></span>').text(value.toUpperCase()));
                        sourceName.append(sourceLink);

                    });
                    tableRow.append(sourceName);
                    tableRow.append($("<td/>").text(mapping.distance))

                    table.append(tableRow);
                }

                var panel = $('<div class="panel panel-default"/>')
                var divPanelHead = $('<div class="panel-heading"/>');
                var requestDisplay = requestId;
                if (requestLabel) {
                    requestDisplay= requestId + " (" +requestLabel + ")"
                }
                var panelTitle = $('<h3 class="panel-title">Mappings to <a href="/terms/'+requestId+'">'+requestDisplay+'</a></h3> ');
                // var divPanelBody= $('<div class="panel-body"/>');


                divPanelHead.append(panelTitle);
                // divPanelBody.append(table);
                panel.append(divPanelHead);

                if (res.length > 0) {
                    panel.append(table);
                }
                else {
                    panel.append("<p style='font-size: larger;color:red;'>No mappings found</p>")
                }


                $("#mapping-vis-spinner").hide();

                $(this).append(panel);


            },
            error : handleAjaxError
        });
    });
}

/**
 * private method used by the advanced search interface to render
 * target mapping values in the facet
 * @param value
 * @private
 */

var targetSources = {}
var mappingSources = {}

function _mappingTargetFound (value) {

    if($("#datasource-summary").length) {

        if ($.isEmptyObject(targetSources)) {
            var fieldList = $('<div id="mapping-target-list" class="list-group"></div>');
            $("#datasource-summary").append(fieldList);
        }

        if (!targetSources[value]) {
            var button = $('<button type=\'button\' id="' + value + '" class="type_list list-group-item mapping-target-list-item">' + value + '<span class="badge mapping-target-'+ value.toLowerCase()+'">' + 1 + '</span></button>');
            $("#mapping-target-list").append(button);

            // make button clickable
            button.on('click', function(e){
                $(".datasource-select").val(e.target.id.toLowerCase());
                $("#filter-form").submit();
            });

            targetSources[value] = 1;
        } else {
            targetSources[value]++;
            $('.mapping-target-'+value.toLowerCase()).text(targetSources[value])


        }

        var $wrapper = $('#mapping-target-list');
        $wrapper.find('.mapping-target-list-item').sort(function(a, b) {
                return +$(b).find("span").text() - +$(a).find("span").text();
            })
            .appendTo($wrapper);

    }

}

function _mappingSourceFound (value) {

    if($("#mapping-summary").length) {

        if ($.isEmptyObject(mappingSources)) {
            var fieldList = $('<div id="mapping-source-list" class="list-group"></div>');
            $("#mapping-summary").append(fieldList);
        }

        if (!mappingSources[value]) {
            var button = $('<button type=\'button\' id="' + value + '" class="type_list list-group-item mapping-source-list-item">' + value + '<span class="badge mapping-source-'+ value.toLowerCase()+'">' + 1 + '</span></button>');
            $("#mapping-source-list").append(button);

            // make button clickable
            button.on('click', function(e){
                $(".mapping-source-select").val(e.target.id.toLowerCase());
                $("#filter-form").submit();
            });

            mappingSources[value] = 1;
        } else {
            mappingSources[value]++;
            $('.mapping-source-'+value.toLowerCase()).text(mappingSources[value])


        }

        var $wrapper = $('#mapping-source-list');
        $wrapper.find('.mapping-source-list-item').sort(function(a, b) {
                return +$(b).find("span").text() - +$(a).find("span").text();
            })
            .appendTo($wrapper);

    }
}


function handleAjaxError (jqXHR, textStatus, errorThrown) {
    console.log("error: " + textStatus + " and " + errorThrown + " and " + jqXHR )

    var panel = $('<div class="panel panel-default"/>')


    $("#mapping-vis-spinner").hide();

}

function clearFilter() {
    $('#distance-slider').slider('setValue', 3, false, false);
    $('#mapping-source-select').val('');
    $('#datasource-select').val('');
    $('#filter-form').submit();

}

function clearAll() {
    $('#identifiers').val("");
}

function populateExamples() {
    $('#identifiers').val("EFO:0001360\nDOID:162\nOMIM:180200\nMESH:D009202\nUBERON_0002107\nHP_0005978");
}

function exportData(format) {

    if ($('#filter-form')) {

        var params = $('#filter-form').serialize()

        if (format == 'csv') {
            params += "&format=csv"
        }
        if (format == 'tsv') {
            params += "&format=tsv"
        }

        window.location = '../api/search?' + params

    }

}