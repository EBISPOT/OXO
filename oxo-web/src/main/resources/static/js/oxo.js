$(document).ready(function() {


    
    $( "div[data-get-mapping]" ).each(function() {


        var id = $(this).data("get-mapping");

        console.log("to map: "  +id)

        $.ajax({
            url:"/api/search",
            dataType: 'json',
            method: 'POST',
            data : {identifiers : id},
            context: this,
            success:function(data) {
                var key = Object.keys(data)[0];

                var res = data[key]
                var table = $("<table style='width:550px;'/>");
                var headerRow = $("<tr/>")
                headerRow.append($("<th style='width:250px;'>Id</th><th style='width:150px;'>Sources</th><th style='width:150px;'>Distance</th>"))
                table.append(headerRow);

                for (var x=0;x<res.length;x++) {
                    var tableRow = $("<tr/>")
                    var mapping = res[x];

                    var termLink = $('<a class="nounderline" style="border-bottom-width: 0px;"></a>').attr('href', '/terms/' + mapping.curie);
                    var curie =  $('<span class="term-source"></span>').text(mapping.curie)


                    var term =  $("<td/>");
                    termLink.append(curie);
                    term.append(termLink);

                    if (mapping.label != undefined) {
                        term.append(" (" + mapping.label + ")");
                    }

                    tableRow.append(term)
                    var sourceName =    $("<td/>");
                    $.each(mapping.sourcePrefixes, function (index, value){
                        var sourceLink = $('<a class="nounderline" style="border-bottom-width: 0px;"></a>').attr('href', '/datasources/' + value);

                        sourceLink.append($('<span class=\"ontology-source\"></span>').text(value.toUpperCase()));
                        sourceName.append(sourceLink);

                    });
                    tableRow.append(sourceName);
                    tableRow.append($("<td/>").text(mapping.distance))

                    table.append(tableRow);
                }
                $(this).append(table)

            }
        });




    });

});


function clearAll() {
    $('#identifiers').val("");
}

function populateExamples() {
    $('#identifiers').val("EFO:0001360\nDOID:162\nOMIM:180200\nMESH:D009202\nUBERON_0002107\nHP_0005978");
}