
var link;
var node;

$(document).ready(function() {

    $("#mapping-vis-spinner").show();
    var sourcePrefixParams = $("#mapping-vis").data("datasource-prefix") ?  '?datasource='+$("#mapping-vis").data("datasource-prefix") : '';
    var apiPath = $("#mapping-vis").data("api-path") ?  $("#mapping-vis").data("api-path") : '';

    
    var diameter = 600,
        radius = diameter / 2,
        innerRadius = radius - 120;

    var cluster = d3.layout.cluster()
        .size([360, innerRadius])
        .sort(null)
        .value(function(d) { return d.size; });

    var bundle = d3.layout.bundle();

    var line = d3.svg.line.radial()
        .interpolate("bundle")
        .tension(.85)
        .radius(function(d) { return d.y; })
        .angle(function(d) { return d.x / 180 * Math.PI; });

    var svg = d3.select("#mapping-vis").append("svg")
        .attr("width", diameter)
        .attr("height", diameter)
        .append("g")
        .attr("transform", "translate(" + radius + "," + radius + ")");

    link = svg.append("g").selectAll(".link"),
    node = svg.append("g").selectAll(".node");

    d3.json(apiPath+"api/mappings/summary"+sourcePrefixParams, function (error, classes) {

        if (error) throw error;

        var nodes = cluster.nodes(packageHierarchy(classes)),
            links = packageImports(nodes);

        link = link
            .data(bundle(links))
            .enter().append("path")
            .each(function(d) { d.source = d[0], d.target = d[d.length - 1]; })
            .attr("class", "link")
            .attr("d", line);

        node = node
            .data(nodes.filter(function(n) { return !n.children; }))
            .enter().append("text")
            .attr("class", "node")
            .attr("dy", ".31em")
            .attr("transform", function(d) { return "rotate(" + (d.x - 90) + ")translate(" + (d.y + 8) + ",0)" + (d.x < 180 ? "" : "rotate(180)"); })
            .style("text-anchor", function(d) { return d.x < 180 ? "start" : "end"; })
            .text(function(d) { return d.key; })
            .on("mouseover", mouseovered)
            .on("mouseout", mouseouted)
            .on("click", function(d) {window.location =  apiPath+"datasources/" + d.key});

        $("#mapping-vis-spinner").hide();

    });
    d3.select('#mapping-vis').style("height", diameter + "px");

});


function mouseovered(d) {
    node
        .each(function(n) { n.target = n.source = false; });

    link
        .classed("link--target", function(l) { if (l.target === d) return l.source.source = true; })
        .classed("link--target", function(l) { if (l.source === d) return l.target.target = true; })
        .filter(function(l) { return l.target === d || l.source === d; })
        .each(function() { this.parentNode.appendChild(this); });

    node
        .classed("node--source", function(n) { return n.target; })
        .classed("node--source", function(n) { return n.source; });
}

function mouseouted(d) {
    link
        .classed("link--target", false)
        .classed("link--target", false);

    node
        .classed("node--source", false)
        .classed("node--source", false);
}


// Lazily construct the package hierarchy from class names.
function packageHierarchy(classes) {

    var map = {};

    function find(name, data) {
        var node = map[name];
        var i;
        if (!node) {
            node = map[name] = data || {name: name, children: []};
            if (!node.name) {
                node.name= name;
                node.children = []
            }
            if (name.length) {

                node.parent = find(name.substring(0, i = name.lastIndexOf(".")));
                node.parent.children.push(node);
                node.key = name.substring(i + 1);
            }
        }
        return node;
    }


    var parentNode = {name: "", children: []}
    map[""] = parentNode;
    
    var edgesMap = {};
    
    classes.forEach(function(d) {

        if (!map[d.source]) {
            if (!map[d.sourceType]) {
                map[d.sourceType] = {key: d.sourceType, name: d.sourceType, children: [], parent : parentNode};
                map[""].children.push(map[d.sourceType])
            }
            map[d.source] = {key: d.source, name: d.source, children: [], parent : map[d.sourceType], edge: [ {target : d.target, source : d.source }] } ;
            map[d.sourceType].children.push(map[d.source])                                                                                                          
        } else {
            map[d.source].edge.push ({target : d.target, source : d.source, size : d.size })
        }

        if (!map[d.target]) {
            if (!map[d.targetType]) {
                map[d.targetType] = {key: d.targetType, name: d.targetType, children: [], parent : parentNode};
                map[""].children.push(map[d.targetType])
            }
            map[d.target] = {key: d.target, name: d.target, children: [], parent : map[d.targetType], edge: [ {target : d.source, source : d.target  }]};
            map[d.targetType].children.push(map[d.target])
        } else {
            // console.log("Adding " + d.target + " -> " + d.source)
            map[d.target].edge.push ({target : d.source, source : d.target, size : d.size })
        }

        // find(d.source, d);

        // find2(d.target, d);
    });

    return map[""];
}

// Return a list of imports for the given array of nodes.
function packageImports(nodes) {
    var map = {},
        imports = [];

    // Compute a map from name to node.
    nodes.forEach(function(d) {
        map[d.name] = d;
    });

    // For each import, construct a link from the source to target node.
    nodes.forEach(function(d) {
        if (d.edge) {
            d.edge.forEach(function (edge) {
                imports.push({source : map[edge.source], target : map[edge.target]  });
                
            });
        }
    });

    return imports;
}