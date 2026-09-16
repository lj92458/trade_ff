let avgDiff_maxCell = 500;
$(document).ready(function () {
    initPage();
    load(1, avgDiff_maxCell);


});

/**
 * 方法：show 显示图表
 * 参数：
 * legend_dataArr 数据分类
 * xAxis_dataArr x坐标数据
 * seriesArr y坐标数据
 */
let showEchart = function (legend_dataArr, xAxis_dataArr, seriesArr) {
    let myChart = echarts.init(document.getElementById('main'));

// 指定图表的配置项和数据
    let option = {
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

let load = function (timeUnit, maxCell) {
    $.getJSON(contextPath + '/engine/queryDiffPrice',
        {unit: timeUnit, maxCell: maxCell},
        function (data) {
            if (data.retCode !== '0000') {// 如果有异常消息
                alert(data.retCode + ':' + data.retMsg);
            } else {
                showEchart(data.legend, data.xAxis, data.series);
                $('#balance').text('总收入：' + data.totalEarn + ',最近收入：' + data.thisEarn);
                $('#retMsg').html(data.engineState);
                // 填充文本框
                let html = '<td>adjPrice：</td>\n'
                for (let platName of data.legend) {
                    html += '<td>' + platName + ':</td>\n' +
                        ' <td> <input type="text" style="width: 40px" name="price" id="price_' + platName + '" value="' + data.price[platName] + '"  >' +
                        '<span></span> </td>\n'
                }
                $('#tr_price').html(html);

                //调节平台的goods占比
                html = '<td>pgoods：</td>'
                for (let platName of data.legend) {
                    html += '<td>' + platName + '</td>\n' +
                        ' <td> <input type="text" style="width: 40px" name="pgoods" id="pgoods_' + platName + '" value="' + data.pgoods[platName] + '"  >' +
                        '<span></span> </td>\n'
                }
                $('#tr_pgoods').html(html )

                //调节平台的money占比
                html = '<td>pmoney：</td>'
                for (let platName of data.legend) {
                    html += '<td>' + platName + '</td>\n' +
                        ' <td> <input type="text" style="width: 40px" name="pmoney" id="pmoney_' + platName + '" value="' + data.pmoney[platName] + '"  >' +
                        '<span></span> </td>\n'
                }
                $('#tr_pmoney').html(html )

                //goods价值占总投资额的比例
                html = '<td>goodsRate：</td>'
                html += '<td>各平台总量</td>\n' +
                    ' <td> <input type="text" style="width: 40px" name="goodsRate" id="goodsRate_' + '' + '" value="' + data.goodsRate + '"  >' +
                    '<span></span> </td>\n' +
                    '<td></td>'.repeat((data.legend.length - 1) * 2)

                $('#tr_goodsRate').html(html )
            }// end else

        });
};


let initPage = function () {
    //设置偏差
    $('#set_price').bind('click', () => setX('price'))
    $('#set_pgoods').bind('click', () => setX('pgoods'))
    $('#set_pmoney').bind('click', () => setX('pmoney'))

    //设置goodsRate
    $('#set_goodsRate').bind('click', function () {
        $.post(contextPath + '/engine/goodsRate',
            {goodsRate: $('#goodsRate').val()},
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
                load(1, avgDiff_maxCell);
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
    $('#shutdown').bind('click', function () {
        if (window.confirm('要结束tomcat吗？\n结束后，只能在控制台重启!!!')) {
            $.post(contextPath + '/actuator/shutdown',
                {},
                function (data) {
                    $('#retMsg').html(data.retMsg);
                },
                'json'
            );
        }
    });
    $('#logfile').bind('click', function () {
        $.ajax(contextPath + '/actuator/logfile', {
            method: 'get',
            success: function (text) {
                let newPage = window.open("about:blank", "_blank");
                newPage.document.body.innerText = text;
            },
            dataType: 'text',
            headers: {Range: 'bytes=-25600'} //http协议中的range协议，bytes=m-n
        });

    });
    //不同的时间间隔
    $('.timeGape').bind('click', function () {
        load($(this).attr('title'), avgDiff_maxCell);

    });
}

/**
 * 设置price，pgoods , pmoney
 * @param x price，pgoods , pmoney
 */
function setX(x) {
    $('#set_' + x).bind('click', function () {
        //收集value
        let xStr = '';
        let inputArr = $('input[name=' + x + ']');
        for (let input of inputArr) {
            xStr += ',';
            xStr += $(input).attr('id').split('_')[1] + ':' + $(input).val();
        }
        $.post(contextPath + '/engine/adjust',
            {key: x, value: xStr.slice(1)},
            function (data) {
                $('#retMsg').html(data.retMsg);
            },
            'json'
        );
    });
}