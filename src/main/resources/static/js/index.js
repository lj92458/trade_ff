var avgDiff_maxCell = 500;
$(document).ready(function () {
    initPage();
    load(5, avgDiff_maxCell);


});

/**
 * 方法：show 显示图表
 * 参数：
 * legend_dataArr 数据分类
 * xAxis_dataArr x坐标数据
 * seriesArr y坐标数据
 */
var showEchart = function (legend_dataArr, xAxis_dataArr, seriesArr) {
    var myChart = echarts.init(document.getElementById('main'));

// 指定图表的配置项和数据
    var option = {
        title: {
            text: '价格偏差走势图'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: legend_dataArr
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        toolbox: {
            feature: {
                saveAsImage: {}
            }
        },
        xAxis: {
            type: 'time',
            //boundaryGap:['20%','20%'] , //

        },
        yAxis: {
            type: 'value'
        },
        series: seriesArr
    };

    // 使用刚指定的配置项和数据显示图表。
    myChart.setOption(option);
}//end function

var load = function (timeUnit, maxCell) {
    $.getJSON(contextPath + '/engine/queryDiffPrice',
        {unit: timeUnit, maxCell: maxCell},
        function (data) {
            if (data.retCode != '0000') {// 如果有异常消息
                alert( data.retCode + ':' + data.retMsg);
            } else {
                showEchart(data.legend, data.xAxis, data.series);
                $('#balance').text('总收入：' + data.totalEarn + ',最近收入：' + data.thisEarn);
                $('#retMsg').html(data.engineState);
                // 填充文本框
                $('#table_adjPrice').html('');
                for (var i = 0; i < data.legend.length; i++) {
                    var platName = data.legend[i];
                    $('#table_adjPrice').append(
                        '<tr>' +
                        '<td>' + platName + ':</td>\n' +
                        ' <td> <input type="text" name="adjustPrice" id="' + platName + '" value="' + data.adjustPrice[platName] + '"  >' +
                        '<span></span> </td>\n' +
                        '</tr>\n'
                    );
                }


            }// end else

        });
};


var initPage = function () {

    //设置偏差
    $('#setPrice').bind('click', function () {
        //收集价格
        var priceStr = '';
        var inputArr = $('input[name="adjustPrice"]');
        for (var i = 0; i < inputArr.length; i++) {
            priceStr += ',';
            priceStr += $(inputArr[i]).attr('id') + ':' + $(inputArr[i]).val();
        }
        priceStr = priceStr.substr(1);
        $.post(contextPath + '/engine/adjustPrice',
            {
                adjustPrice: priceStr

            },
            function (data) {
                $('#retMsg').html(data.retMsg);
            },
            'json'
        );
    });
    //启动
    $('#start').bind('click', function () {
        $('#retMsg').html('正在启动...请等待5秒');
        $.post(contextPath + '/engine/start',
            {},
            function (data) {
                $('#retMsg').html(data.retMsg);
            },
            'json'
        );

    });
    //停止
    $('#stop').bind('click', function () {
        $.post(contextPath + '/engine/stop',
            {},
            function (data) {
                $('#retMsg').html(data.retMsg);
            },
            'json'
        );

    });

    //不同的时间间隔
    $('.timeGape').bind('click', function () {
        load($(this).attr('title'), avgDiff_maxCell);

    });
}