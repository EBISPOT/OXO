$(document).ready(function() {

    initialisePage()
});


var table;
function reinitialiseTable() {
    table.destroy()
    initialisePage()
}

var withProgress = true;

var hideTableInfo = true;
var hideFromCol = false;
var apiPath = '';

function initialisePage() {

    withProgress =  $("#example").data("with-progress") ? $("#example").data("with-progress") : true;
    hideTableInfo =  $("#example").data("hide-table-info") ?  $("#example").data("hide-table-info"): false;
    hideFromCol =  $("#example").data("hide-from-col") ?  $("#example").data("hide-from-col"): false;

    apiPath = $("#example").data("api-path") ? $("#example").data("api-path"): '';
    console.log(apiPath)
    if (withProgress) {
        addProgressBar();
    }

    // need to determine if this is a search by id or search by datasource

    // get any input ids (these take precedence
    var values = $("input[name=ids]").val() ? $("input[name=ids]")
        .map(function(){return $(this).val();}).get()  : [];

    // if no id, for now the UI only supports search with an input data source and a target ontology
    var inputSource = $("input[name=inputSource]").val() ? $("input[name=inputSource]").val() : undefined;
    var mappingTarget = $("input[name=mappingTarget]").val() ? $("input[name=mappingTarget]").map(function(){return $(this).val();}).get() : [];
    var distance = $("input[name=distance]").val() ? $("input[name=distance]").val() : 1;

    // construct the API request object

    var requestData = {

        ids : values,
        inputSource :  inputSource,
        mappingTarget: mappingTarget,
        distance: distance
    };

    var resultsData = [];
    doSearch(apiPath+'api/search?size=500', requestData, resultsData)

}

function doSearch(url, requestData, resultsData, noMappings) {

    if (!noMappings) {
        noMappings = 0;
    }
    $.ajax({
        url: url,
        dataType: 'json',
        method: 'POST',
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(requestData),
        context: this,
        success: function (data) {

            var pageNumber = data.page.number;
            var totalPages = data.page.totalPages;
            var totalElement = data.page.totalElements;
            if (totalElement > 50000) {
                $('#large-warning').show()
            }
            
            $.each(data._embedded.searchResults, function (index, result) {

                var mappings = result.mappingResponseList;

                var requestId = result.curie;
                var requestLabel = result.label != 'null' ? result.label : undefined;

                if (mappings.length == 0) {
                    var row = [result.queryId, '','No mapping', '' ,'', '','' ,''];
                    resultsData.push(row);
                    noMappings += 1;
                }
                $.each(mappings, function (index, mapping) {

                    var targetSource = mapping.targetPrefix;
                    var mappingId = mapping.curie;
                    var mappingLabel = mapping.label != 'null' ? mapping.label : undefined;
                    var mappingSources = mapping.sourcePrefixes;
                    var mappingSourcesLength = mapping.sourcePrefixes.length;
                    var distance = mapping.distance;

                    var row = [
                        requestId,
                        requestLabel,
                        mappingId,
                        mappingLabel,
                        targetSource,
                        mappingSources,
                        mappingSourcesLength,
                        distance
                    ];
                    resultsData.push(row);
                })

            });

            // update progress bar here
            updateProgress(parseInt(pageNumber / totalPages * 100));

            if (data._links.next) {
                doSearch(data._links.next.href, requestData, resultsData, noMappings)
            }
            else {
                // progress complete
                updateProgress(100);
                progressComplete();
                // $('#mappings-count').text(resultsData.length - noMappings)
                if (noMappings > 0) {
                    $('#unmapped').text(noMappings)
                    $('#mapping-summary').show()
                }

                drawTable(resultsData)
            }
        }
    });

}


function drawTable(resultsData) {

    dom = '<"top"if>rt<"bottom"lp><"clear">'

    if (hideTableInfo) {
        dom = 'rt';
    }

    var hide =  [ 1, 3, 5 ];
    if (hideFromCol) {
        hide =  [ 0, 1, 3, 5 ];

    }

    table = $('#example').DataTable({
        "displayLength": 50,
        "initComplete": function () {
            this.api().columns([4, 7]).every( function () {
                var column = this;
                var select = $('<select><option value=""></option></select>')
                    .appendTo( $(column.header()).append($('<br/>')) )
                    .on( 'change', function () {
                        var val = $.fn.dataTable.util.escapeRegex(
                            $(this).val()
                        );
                        column
                            .search( val ? '^'+val+'$' : '', true, false )
                            .draw();
                    } )
                    .on( 'click', function(e) {
                        e.stopPropagation();
                    });
                column.data().unique().sort().each( function ( d, j ) {
                    select.append( '<option value="'+d+'">'+d+'</option>' )
                } );
            } );
            // handle column 6 (provenance)
            var provenanceColumn = this.api().columns([5]);
            this.api().columns([6]).every(function () {
                var column = this;
                var select = $('<select><option value=""></option></select>')
                    .appendTo( $(column.header()).append($('<br/>')) )
                    .on( 'change', function () {
                        var val = $.fn.dataTable.util.escapeRegex(
                            $(this).val()
                        );
                        provenanceColumn
                            .search( val ? val : '', true, false)
                            .draw();
                    } )
                    .on( 'click', function(e) {
                        e.stopPropagation();
                    });

                provenanceColumn.data().unique().sort().each( function ( d, j ) {
                    var uniq = {}
                    $.each(d, function (index, val) {
                        $.each(val, function (i1, val1) {
                            if (!uniq[val1]) {
                                select.append( '<option value="'+val1+'">'+val1+'</option>' )
                                uniq[val1] = 1
                            }
                        });
                    })
                } );
            } );

        },
        "deferRender": true,
        'dom': dom,
        "columnDefs": [
            {
                "render": function ( data, type, row ) {
                    return renderId(data, row[1]);
                },
                "width": "20%",
                "targets": 0
            },
            {
                "render": function ( data, type, row ) {
                    return renderId(data, row[3]);
                },
                "width": "20%",
                "targets": 2
            },
            {
                "render": function ( data, type, row ) {
                    return renderDatasource(data);
                },
                "width": "10%",
                "targets": 4
            },
            {
                "render": function ( data, type, row ) {
                    return renderMappingCount(data, row[0], row[2]);
                },
                "width": "10%",
                "targets": 6
            },
            {
                "width":"10%",
                "targets": 7
            },
            { "visible": false,  "targets": hide }
        ],
        data: resultsData,
        columns: [
            { title: "Input" },
            { title: "Input label" },
            { title: "Mapped id" },
            { title: "Mapped label" },
            { title: "Id source" },
            { title: "Evidence list" },
            { title: "Evidence" },
            { title: "Distance" }
        ]
    } );
}

function renderId(id, label) {

    var cell = $('<span/>');

    var targetLink = $('<a class="nounderline" target="_blank" style="border-bottom-width: 0px;"></a>').attr('href', apiPath+'terms/' + id);
    var curie =  $('<span class="term-source"></span>').text(id)

    if (id == 'No mapping') {
        targetLink = $('<span></span>');
        curie =  $('<span class="no-mapping"></span>').text(id)
    }


    targetLink.append(curie);
    cell.append(targetLink)
    if (label) {
        cell.append (" (" + label + ")")
    }
    return cell.html();

}

function renderDatasource(targetSource) {
    var cell = $('<span/>');
    var targetLink = $('<a class="nounderline" target="_blank" style="border-bottom-width: 0px;"></a>').attr('href', apiPath+'datasources/' + targetSource);
    var targetSpan =  $('<span class="ontology-source"></span>').text(targetSource)

    targetLink.append(targetSpan);
    cell.append(targetLink)
    return cell.html();
}

function renderMappingCount(count, from, to) {
    var cell = $('<span/>');
    var sourceLink = $('<a/>',
        {
            href : apiPath+'mappings?fromId=' + from + '&' + 'toId=' + to,
            text :  count,
            target : "_blank"
        }
    );
    cell.append(sourceLink)
    return cell.html();
}


function getApiPath(element) {
    return $(element).data("api-path") ? $(element).data("api-path") : '';
}

function progressComplete() {
    if (withProgress) {
        $( ".progress-label" ).text( "Complete!" );
    }
}

function addProgressBar() {

    var progressbar = $( "#progressbar" ),
        progressLabel = $( ".progress-label" );

    progressbar.progressbar({
        value: false,
        change: function() {
            progressLabel.text( progressbar.progressbar( "value" ) + "%" );
        },
        complete: function() {
            progressLabel.text( "Complete!" );
        }
    });

}

function updateProgress(value) {
    if (withProgress) {
        $("#progressbar").progressbar( "value", value)
    }
}
