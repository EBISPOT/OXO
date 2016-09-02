


var colorMap={"efo": "blue", "uberon":"green", "Wikipedia": "red", "BTO": "yellow", "XAO":"black", "ZFA":"gray"}


$(document).ready(function() {

    // $("#mapping-vis-spinner").show();
    var curie = $("div[data-get-mapping-vis]").data('get-mapping-vis');
    console.log("Getting vis for " + curie)


    console.log("Ready!")

    $.getJSON("http://localhost:8080/api/terms/"+curie+"/graph", function(json) {})
        .success(function(json){
            console.log("Loaded data")

            console.log(json)
            var container = document.getElementById('mynetwork');



            for(var i=0;i<json.nodes.length;i++){
                json.nodes[i]["label"]=json.nodes[i]["id"]
                json.nodes[i]["color"]=colorMap[json.nodes[i]["group"]]
                json.nodes[i]["shape"]="box"
            }

            for(var i=0;i<json.links.length; i++){
                json.links[i]["arrows"]='to'
                json.links[i]["color"]=colorMap[json.links[i]["mappingSource"]]
                json.links[i]["to"]=   json.links[i]["target"]
                json.links[i]["from"]=   json.links[i]["source"]

                console.log(json.links[i]["color"])
                if (json.links[i]["color"]===undefined){
                    console.log("Undefined ontology found: "+json.links[i]["mappingSource"])
                }
            }

            var nodes = new vis.DataSet(json.nodes)
            var edges = new vis.DataSet(json.links)


            var data = {
                nodes: nodes,
                edges: edges
            };


            console.log(nodes)
            console.log(edges)
            console.log(data)

            var options = {};
            var network = new vis.Network(container, data, options);

            // $("#mapping-vis-spinner").hide();

        })
        .fail(function(e){console.log(e);console.log("Webservice call did not work!")})
    ;
}) ;


