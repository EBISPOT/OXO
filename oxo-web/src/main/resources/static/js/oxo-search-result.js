$(document).ready(function() {

    initialisePage()
});



function initialisePage() {

    addProgressBar();

    // need to determine if this is a search by id or search by datasource

    // get any input ids (these take precedence
    var values = $("input[name=ids]").val() ? $("input[name=ids]")
        .map(function(){return $(this).val();}).get()  : [];

    // if no id, for now the UI only supports search with an input data source and a target ontology
    var inputSource = $("input[name=inputSource]").val() ? $("input[name=inputSource]").val() : undefined;
    var mappingTarget = $("input[name=mappingTarget]").val() ? $("input[name=mappingTarget]").map(function(){return $(this).val();}).get() : [];

    // construct the API request object

    var requestData = {

        ids : values,
        inputSource :  inputSource,
        mappingTarget: mappingTarget,
        distance: 3     // default to 3 but todo allow users to change
    };

    var resultsData = [];
    doSearch('api/search?size=100', requestData, resultsData)

}

function doSearch(url, requestData, resultsData) {

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

            $.each(data._embedded.searchResults, function (index, result) {

                var mappings = result.mappingResponseList;

                var requestId = result.curie;
                var requestLabel = result.label;

                if (mappings.length == 0) {
                    var row = [requestId + " (" + requestLabel + " )",'' ,'' ,'' ,''];
                    resultsData.push(row);
                }
                $.each(mappings, function (index, mapping) {

                    var targetSource = mapping.targetPrefix;
                    var mappingId = mapping.curie;
                    var mappingLabel = mapping.label;
                    var mappingSourcesLength = mapping.sourcePrefixes.length;
                    var distance = mapping.distance;

                    var row = [
                        requestId + " (" + requestLabel + " )",
                        mappingId + " (" + mappingLabel + " )",
                        targetSource,
                        mappingSourcesLength,
                        distance
                    ];
                    resultsData.push(row);
                })

            });

            // update progress bar here
            updateProgress(parseInt(pageNumber / totalPages * 100));

            if (data._links.next) {
                doSearch(data._links.next.href, requestData, resultsData)
            }
            else {
                // progress complete
                updateProgress(100);
                progressComplete();
                drawTable(resultsData)
            }
        }
    });

}


function drawTable(resultsData) {
    var table = $('#example').DataTable({
        "displayLength": 50,
        "deferRender": true,
        // "columnDefs": [
        //     { "visible": false, "targets": 0 }
        // ],
        // "order": [[ 0, 'asc' ]],
        // "displayLength": 25,
        // "drawCallback": function ( settings ) {
        //     var api = this.api();
        //     var rows = api.rows( {page:'current'} ).nodes();
        //     var last=null;
        //
        //     api.column(0, {page:'current'} ).data().each( function ( group, i ) {
        //         if ( last !== group ) {
        //             $(rows).eq( i ).before(
        //                 '<tr class="group"><td colspan="5">'+group+'</td></tr>'
        //             );
        //
        //             last = group;
        //         }
        //     } );
        // },

        data: resultsData,
        columns: [
            { title: "fromId" },
            { title: "toId" },
            { title: "target" },
            { title: "no. of mappings" },
            { title: "distance" }
        ]
    } );
}


function getApiPath(element) {
    return $(element).data("api-path") ? $(element).data("api-path") : '';
}

function progressComplete() {
    $( ".progress-label" ).text( "Complete!" );
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
    $("#progressbar").progressbar( "value", value)
}
