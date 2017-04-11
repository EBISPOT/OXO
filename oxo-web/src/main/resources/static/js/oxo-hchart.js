/**
 * Created by jupp on 02/03/2017.
 */

$(document).ready(function() {

    refreshChartData()
});

function refreshChartData() {
    $("#graphic").html('');
    $("#mapping-vis").show()

    var prefix =  $("#graphic").data("prefix");
    var distance = $("input[name=distance]").val() ? $("input[name=distance]").val() : 1;

    $.ajax({
        url: '../api/mappings/summary/counts?datasource='+prefix+'&distance='+distance,
        dataType: 'json',
        method: 'GET',
        contentType: "application/json; charset=utf-8",
        context: this,
        success: function (data) {
            drawChart(data)
        }
    });
}

function drawChart(data) {

    var reformatted = [];

    // console.log(Object.keys(data))
    $.each(data,function(key,value){
        reformatted.push( [key, value])
        $("#mapping-vis").hide()
    });

    console.log(JSON.stringify(reformatted))

    var chart = Highcharts.chart('graphic', {
        chart: {
            type: 'bar'
        },
        credits:{
            enabled:false
        },
        title: {
            text: 'Mappings by target'
        },
        xAxis: {
            type: 'category',
            labels: {
                // rotation: -45,
                style: {
                    fontSize: '13px',
                    fontFamily: 'Verdana, sans-serif'
                },
                formatter: function(){
                    return "<a style='border-bottom-width: 0px;' class='nounderline' href='"+this.value +"'><span class='ontology-source'>" + this.value + "</span></a>"
                },
                useHTML: true,
                step : 1
            },
            categories: Object.keys(data),

        },
        yAxis: {
            min: 0,
            title: {
                text: 'Number of mappings'
            }
        },
        legend: {
            enabled: false
        },
        plotOptions: {
            series: {
                cursor: 'pointer',
                point: {
                    events: {
                        click: function () {
                            // console.log('clicked' + this.category)
                            $('#mappingTarget').val(this.category)
                            $('#mapping-count-form').submit()
                        }
                    }
                },
                minPointLength: 3,
                events: {
                    legendItemClick: function(ev) {
                        // console.log(ev.point.category)
                    }
                }
            }
        },
        events: {
            redraw: function(event) {
                console.log("size" + this.xAxis.categories)
            }
        },
        series: [{
            name: 'Mappings',
            data: reformatted
        }]
    });

    var height = reformatted.length * 40;
    if (height < 200) {
        height = 200;
    }
    chart.setSize(undefined, height)

}


