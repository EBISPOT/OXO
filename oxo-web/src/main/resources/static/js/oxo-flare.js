
var link;
var node;

$(document).ready(function() {

    $("#mapping-vis-spinner").show();

    
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

    d3.json("api/mappings/summary", function (error, classes) {

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
            .on("click", function(d) {window.location =  "datasources/" + d.key});

        $("#mapping-vis-spinner").hide();

    });
    d3.select('#mapping-vis').style("height", diameter + "px");

});


function mouseovered(d) {
    node
        .each(function(n) { n.target = n.source = false; });

    link
        .classed("link--target", function(l) { if (l.target === d) return l.source.source = true; })
        .classed("link--source", function(l) { if (l.source === d) return l.target.target = true; })
        .filter(function(l) { return l.target === d || l.source === d; })
        .each(function() { this.parentNode.appendChild(this); });

    node
        .classed("node--target", function(n) { return n.target; })
        .classed("node--source", function(n) { return n.source; });
}

function mouseouted(d) {
    link
        .classed("link--target", false)
        .classed("link--source", false);

    node
        .classed("node--target", false)
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


    classes.forEach(function(d) {
        find(d.source, d);
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
        if (d.target) {
            imports.push({source: map[d.name], target: map[d.target]});
        }
    });

    return imports;
}