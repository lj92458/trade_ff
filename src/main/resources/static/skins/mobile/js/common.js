var isSupportHtml5 = true;  //浏览器是否支持H5
var scrollImgCount = 0;     //首页滚动图片个数

$(document).ready(function(){
    //基础信息初始化
    init();
    //导航菜单 绑定事件
    headNavBindFunc();
    //首页滚动图片
    scrollImgBindFunc();
    //错误类型 绑定事件
    errorEventBindFunc();
    //分页控件 绑定事件
    pagingBoxBindFunc();
    //初始化所有select的值（根据select的value属性）
    initSelectValue();
    //检查cookies支持
    checkCookies();
});

//ajax完成时回调函数
$(document).ajaxComplete(function(event, xhr, settings) {
    //从http头信息取出 在filter定义的sessionstatus，判断是否是 timeout
    if(xhr.getResponseHeader("sessionstatus")==="timeout"){
        //从http头信息取出登录的url ＝ loginPath
        if(xhr.getResponseHeader("loginPath")){
            console.log("会话过期，请重新登陆!");
            //打会到登录页面
            window.location.replace(xhr.getResponseHeader("loginPath"));
        }else{
            alert("请求超时请重新登陆 !");
        }
    }
});
//基础信息初始化
function init(){
    if(!window.applicationCache){
        isSupportHtml5 = false;
    }
    scrollImgCount = $(".scroll-all .image-container li").length;       //滚动图片个数

    FastClick.attach(document.body);        //开启 fastclick，解决手机上click事件响应过慢的问题

}

//头部导航绑定函数
function headNavBindFunc(){
    //绑定按钮事件
    var firstLevelMenus = $(".nav-main .nav-box>li>A").not(".nav-split,.logo-index");
    firstLevelMenus.bind("click", function(){
        //当前 选中/非选中
        var menuObj = $(this).parent();
        if(menuObj.hasClass("selected")){
            menuObj.removeClass("selected");
        }else{
            menuObj.parent().children(".selected").not(".nav-split,.logo-index").removeClass("selected");

            menuObj.addClass("selected");
        }
        //当前菜单 展开/合起
        var subNavBox = menuObj.children(".sub-nav-box");
        if(null != subNavBox && subNavBox.length > 0){
            if(subNavBox.hasClass("hidden")){
                menuObj.parent().children("li").not(".nav-split,.logo-index").children(".sub-nav-box").not(".hidden").addClass("hidden");
                subNavBox.removeClass("hidden");
            }else {
                subNavBox.addClass("hidden");
            }
        }
    });

    //导航菜单显示/隐藏按钮绑定事件（移动版）
    $(".header-content .nav-menu-main A").bind("click", function(){
        var navMain = $(".header-content .nav-main");
        if($(this).hasClass("selected")){
            $(this).removeClass("selected");
            navMain.css("display", "none");
            navMain.find(".nav-box>LI").removeClass("selected");
            navMain.find(".nav-box .sub-nav-box").addClass("hidden");
        }else{
            $(this).addClass("selected");
            navMain.css("display", "block");
        }
    });
    
    //导航弹出层 close
    $(".nav-main .mob-nav-top .nav-close").bind("click", function(){
        var navMain = $(".header-content .nav-main");
        navMain.css("display", "none");
        navMain.find(".nav-box>LI").removeClass("selected");
        navMain.find(".nav-box .sub-nav-box").addClass("hidden");

        $(".header-content .nav-menu-main A").removeClass("selected");
    });
}

/**
 * 首页滚动图片 绑定事件
 */
function scrollImgBindFunc(){
    var scrollPaging = $(".scroll-all .scroll-paging");
    if(!scrollPaging || scrollPaging.length == 0){
        return;
    }
    for(var i=0;i<scrollImgCount;i++){
        if(i==0){
            scrollPaging.append('<em class="selected"></em>');
        }else{
            scrollPaging.append('<em></em>');
        }
    }

    var pagings = $(".scroll-all .scroll-paging em");
    //分页按钮 点击事件
    pagings.click(function(){
        var curIndex = $(".scroll-all .scroll-paging .selected").index();
        var nextIndex = $(this).index();
        //滚动
        isSkipCurScroll = true;     //跳过本次自动轮播
        doChangeScrollImg(curIndex, nextIndex);
    });

    //上一页下一页
    $(".scroll-all .scroll-content .scroll-left-arrow").click(function(){
        var curIndex = $(".scroll-all .scroll-paging .selected").index();
        var nextIndex = curIndex - 1;
        if(nextIndex < 0){
            nextIndex = scrollImgCount -1;
        }
        isSkipCurScroll = true;     //跳过本次自动轮播
        doChangeScrollImg(curIndex, nextIndex);
    });
    $(".scroll-all .scroll-content .scroll-right-arrow").click(function(){
        var curIndex = $(".scroll-all .scroll-paging .selected").index();
        var nextIndex = curIndex + 1;
        if(nextIndex > scrollImgCount -1){
            nextIndex = 0;
        }
        isSkipCurScroll = true;     //跳过本次自动轮播
        doChangeScrollImg(curIndex, nextIndex);
    });

    scrollToNextTimer();
}
var isSkipCurScroll = false;        //是否跳过本次自动轮播
//滚动到下一张图片 定时器
function scrollToNextTimer(){
    setTimeout(function(){
        if(!isSkipCurScroll){
            var curIndex = $(".scroll-all .scroll-paging .selected").index();
            var nextIndex = curIndex + 1;
            if(nextIndex > scrollImgCount -1){
                nextIndex = 0;
            }
            doChangeScrollImg(curIndex, nextIndex);
        }
        isSkipCurScroll = false;
        scrollToNextTimer();
    },5000);
}

function doChangeScrollImg(curIndex, nextIndex){
    var loopCount = 50;
    var loopInterval = 10;       //事件间隔 毫秒
    var loopIndex = 0;
    for(var i=0;i<loopCount;i++){
        setTimeout(function(){
            loopIndex++;
            var imgContainer = $(".scroll-all .image-container");
            if(isSupportHtml5){
                //采用transform 相对流畅
                var curTrans = -curIndex*100/scrollImgCount + (curIndex - nextIndex)*100/scrollImgCount/loopCount * loopIndex;
                imgContainer.css("transform", "translate3d("+ curTrans +"%, 0px, 0px)");
            }else{
                //采用margin-left
                var marginLeft = -curIndex*100 + (curIndex - nextIndex)*100/loopCount * loopIndex;
                imgContainer.css("margin-left", marginLeft +"%");
            }

        },loopInterval * i);
    }

    $(".scroll-all .scroll-paging em").removeClass("selected");
    var pageEm = $(".scroll-all .scroll-paging em").get(nextIndex);
    $(pageEm).addClass("selected");
}


//工具方法：获取元素的纵坐标
function getTop(obj){
    var offset=obj.offsetTop;
    if(obj.offsetParent!=null) offset+=getTop(obj.offsetParent);
    return offset;
}
//工具方法：获取元素的横坐标
function getLeft(obj){
    var offset=obj.offsetLeft;
    if(obj.offsetParent!=null) offset+=getLeft(obj.offsetParent);
    return offset;
}

//更换图形验证码
function changeImgVerifyCode(imgId) {
    var img = $("#"+imgId);
    var src = img.attr("src");
    var timestamp = (new Date()).valueOf();
    if(src.indexOf("?t=") < 0){
        src += "?t=" + timestamp;
    }else{
        var paramIndex = src.indexOf("?t=");
        src = src.substring(0, paramIndex);
        src += "?t=" + timestamp;
    }
    img.attr("src", src);
}

/** 展现错误消息 定义 */
var LONG = new Array();
for(var i=1;i<=200;i++){
    LONG[i+""] = i;
}

/** 最小值限制 */
var MIN = new Array();
for(var i=1;i<=100;i++){
    MIN[i+""] = i;
}
var ERR = new Array();
ERR["NOT_NULL"] = "字段不允许为空";
ERR["EMAIL"] = "请填写电子邮件地址";
ERR["PWD"] = "密码长度为8~30位";
ERR["PWD_STRONG"] = "必须为数字、字母或特殊符号的组合";
ERR["RE_PWD"] = "两次输入密码不一致";
ERR["MOBILE"] = "请输入正确的手机号码";
ERR["MOBILE_EMAIL"] = "请输入手机号或邮箱账号";
ERR["MOBILE_PHONE"] = "请输入手机号或座机号";
ERR["POST"] = "请输入邮政编码（若未知，请输000000）";
ERR["MONEY"] = "请输入金额（保留2位小数）";
ERR["RADIO_REQUIRED"] = "请选择一个选项";




for(var num in LONG){
    ERR["LONG-"+num] = "！字段最大长度"+num;
}
for(var num in MIN){
    ERR["MIN-"+num] = "！字段最小长度"+num;
}


//显示错误信息 msgId 例如 "NOT_NULL"
function ShowError(obj,msgId){
    if(null != obj && null != msgId){
        var errMsg = ERR[msgId];
        if(null == errMsg){
            errMsg = msgId;
        }
        var objParent = obj.parent();
        var errTag = objParent.find("span");
        if(errTag && errTag.length > 0){
            errTag.html(errMsg);
        }else{
            if(errMsg != ""){
                obj.css("border", "1px solid #FF6569");
            }else{
                obj.removeAttr("style");
            }
        }
    }
}

//错误绑定事件
function errorEventBindFunc(){
    var objects = $("form input[type='text'], form input[type='password'], form input[type='radio'], form select, form textarea");
    objects.blur(function(){
        validInput($(this));
    });
    objects.keyup(function(){
        validInput($(this));
    });
}

//obj: jquery 对象
function validInput(obj){
    if(obj == null){
        return false;
    }
    var value = obj.val();
    var isValid = false;
    //非空
    if(obj.hasClass('err-not-null')){
        if(null == value || value == ""){
            isValid = false;
            ShowError(obj,"NOT_NULL");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //密码
    if(obj.hasClass('err-pwd')){
        var reg = RegExp("^(?![0-9]+$)(?![a-zA-Z]+$)([0-9A-Za-z]|[\x21-\x7e]){8,30}$");
        if(value.length < 8 || value.length > 30){
            isValid = false;
            ShowError(obj,"PWD");
        }else if(!reg.test(value)){
            isValid = false;
            ShowError(obj,"PWD_STRONG");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //重复密码
    if(obj.hasClass('err-re-pwd')){
        var pwd = obj.parent().parent().parent().parent().find(".err-pwd").val();
        if(pwd != value){
            isValid = false;
            ShowError(obj,"RE_PWD");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //过长
    for(var num in LONG){
        if(obj.hasClass('err-long-'+num)){
            maxLen = LONG[num];
            errId = "LONG-"+num;
            if(value.length > maxLen){
                isValid = false;
                ShowError(obj,errId);
            }else{
                isValid = true;
                ShowError(obj,"");
            }
        }else{
            isValid = true;
        }
        if(!isValid){return false;}//返回
    }


    //过短
    for(var num in MIN){
        if(obj.hasClass('err-min-'+num)){
            minLen = MIN[num];
            errId = "MIN-"+num;
            if(value.length < minLen){
                isValid = false;
                ShowError(obj,errId);
            }else{
                isValid = true;
                ShowError(obj,"");
            }
        }else{
            isValid = true;
        }
        if(!isValid){return false;}//返回
    }
    //邮政编码
    if(obj.hasClass('err-post')){
        if(null != value && value != "" && !isPostCode(value)){
            isValid = false;
            ShowError(obj,"POST");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //邮件
    if(obj.hasClass('err-email')){
        if(null != value && value != "" && !isEmail(value)){
            isValid = false;
            ShowError(obj,"EMAIL");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //手机
    if(obj.hasClass('err-mobile')){
        if(null != value && value != "" && !isMobile(value)){
            isValid = false;
            ShowError(obj,"MOBILE");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //手机 或 邮件
    if(obj.hasClass('err-mobile-email')){
        if(null != value && value != "" && !isEmail(value) && !isMobile(value)){
            isValid = false;
            ShowError(obj,"MOBILE_EMAIL");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //手机 或 座机
    if(obj.hasClass('err-mobile-phone')){
        if(null != value && value != "" && !isMobile(value) && !isPhone(value) ){
            isValid = false;
            ShowError(obj,"MOBILE_PHONE");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //金额 保留2位小数
    if(obj.hasClass('err-money')){
        if(null != value && value != "" && !isMoney(value)){
            isValid = false;
            ShowError(obj,"MONEY");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回
    //Radio 必选
    if(obj.hasClass('err-radio-required')){
        //如果有name属性，检查该form下此name的radio是否有选中的
        //如果没有name属性，检查该radio是否被选中
        var isChecked = false;
        if(obj.attr("name") && obj.parents("form").length > 0){
            var form = obj.parents("form");
            var radios = form.find("input[name='"+ obj.attr("name") +"']");
            for(var i=0; i<radios.length; i++){
                if($(radios[i]).is(':checked')){
                    isChecked = true;
                }
            }
        }else{
            isChecked = obj.is(':checked');
        }
        if(!isChecked){
            isValid = false;
            ShowError(obj,"RADIO_REQUIRED");
        }else{
            isValid = true;
            ShowError(obj,"");
        }
    }else{
        isValid = true;
    }
    if(!isValid){return false;}//返回

    return true;
}

//验证表单
function ValidForm(form){
    var classStr = ".err-not-null, .err-email, .err-pwd, .err-re-pwd, .err-mobile, .err-mobile-email, .err-mobile-phone, .err-money, .err-radio-required";
    for(var num in LONG){
        classStr += ", .err-long-"+num;
    }
    var ennArr = $(form).find(classStr);
    for(var i=0;i<ennArr.length;i++){
        if(!validInput($(ennArr[i]))){
            //焦点
            //$(ennArr[i]).focus();
            return false;
        }
    }
    //密码传输前加密
    var pwdInputs = $(form).find("input[type='password']");
    for(var i=0;i<pwdInputs.length;i++){
        var curInput = $(pwdInputs.get(i));
        var name = curInput.attr("name");
        if(!name){
            continue;
        }
        curInput.removeAttr("name");
        var valueMd5 = hex_md5(curInput.val());
        var tempHtml = '<input type="hidden" name="'+ name +'" value="'+ valueMd5 +'">';
        $(form).append(tempHtml);
    }
    return true;
}
//是否是手机号
function isPostCode(str){
    var reg = /^[0-9]{6}$/;
    if(reg.test(str)){
        return true;
    }else{
        return false;
    }
}
//是否是邮箱账号
function isEmail(str){
    var reg = /^([a-z0-9A-Z]+[-|_|\.]?)+[a-z0-9A-Z]?@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\.)+[a-zA-Z]{2,}$/;
    if(reg.test(str)){
        return true;
    }else{
        return false;
    }
}
//是否是手机号
function isMobile(str){
    var reg = /^[1][0-9]{10}$/;
    if(reg.test(str)){
        return true;
    }else{
        return false;
    }
}
//是否是电话号
function isPhone(str){
    var reg = /^([0][0-9]{2,3}[\-])?[1-9][0-9]{6,7}$/;
    if(reg.test(str)){
        return true;
    }else{
        return false;
    }
}
//是否是金额
function isMoney(str){
    var reg = /^(([1-9][0-9]{0,8})|0)(\.[0-9]{1,2})?$/;
    if(reg.test(str)){
        return true;
    }else{
        return false;
    }
}


/**
 * 格式化金额（千位逗号隔开）
 * @param s 需要格式化的金额
 * @param n 小数位数
 * @returns {string} 格式化后的金额
 */
function fmtMoney(s, n) {
    n = n > 0 && n <= 20 ? n : 2;
    s = parseFloat((s + "").replace(/[^\d\.-]/g, "")).toFixed(n) + "";
    var l = s.split(".")[0].split("").reverse(), r = s.split(".")[1];
    t = "";
    for (i = 0; i < l.length; i++) {
        t += l[i] + ((i + 1) % 3 == 0 && (i + 1) != l.length ? "," : "");
    }
    return t.split("").reverse().join("") + "." + r;
}

/**
 * 提示消息
 * @param msg
 */
function AlertMsg(msg){
    $("#pop_message_cancel").css('display','none');
    $(".window-model-layer").css('display','inline-block');
    $(".pop-message-win").removeClass("hidden");

    $(".pop-message-win .pop-content").find("p").html(msg);
    //绑定事件
    $("#pop_message_ok").unbind('click').bind('click',function(){
        removeModelLayer();
    });
}



/**
 * 分页控件绑定事件
 */
function pagingBoxBindFunc(){
    var pagingBox = $(".paging-box");
    if(!pagingBox || pagingBox.length == 0){
        return;
    }
    var data = eval("(" + pagingBox.attr("data") + ")");
    var pageIndex = data["pageIndex"] * 1;
    var pageTotal = data["pageTotal"] * 1;
    var html = "";
    if(pageIndex <= 1){
        html += "<em class='previous'>上一页</em>";
    }else{
        var previousIndex = pageIndex -1;
        if(previousIndex < 1){
            previousIndex = 1;
        }
        html += "<a href='javascript:void(0)' class='previous' onclick='turnPage(this,\"" +previousIndex+ "\")'>上一页</a>";
    }

    for(var i=1; i<=pageTotal; i++){
        var tempHtml = "";
        if(i == pageIndex){
            tempHtml += "<em class='current'>" +i+ "</em>";
        }else{
            tempHtml += "<a href='javascript:void(0)' onclick='turnPage(this,\""+ i +"\")'>" +i+ "</a>";
        }

        if(i == 1 || i == 2){
            html += tempHtml;
        }else if(i == pageIndex -1 || i == pageIndex || i == pageIndex +1){
            html += tempHtml;
        }else if(i == pageIndex-2 && pageIndex-2 > 2){
            html +="<em>…</em>";
        }else if(i == pageIndex+2 && pageIndex+2 <pageTotal - 1){
            html += "<em>…</em>";
        }else if(i == pageTotal - 1 || i == pageTotal){
            html += tempHtml;
        }
    }
    if(pageIndex >= pageTotal){
        html += "<em class='next'>下一页</em>";
    }else{
        var nextIndex = pageIndex + 1;
        if(nextIndex > pageTotal){
            nextIndex = pageTotal;
        }
        html += "<a href='javascript:void(0)' class='next' onclick='turnPage(this,\""+ nextIndex +"\")'>下一页</a>";
    }
    pagingBox.html(html);
}

function turnPage(obj, pageIndex){
    var form = $(obj).parents("form");
    form.find("input[name='page']").val(pageIndex);
    form.submit();
}


/**
 * 初始化所有select的值
 */
function initSelectValue(){
    var selects = $("select");
    if(!selects || selects.length == 0){
        return;
    }
    selects.each(function(index){
        if($(this).attr("value")){
            $(this).val($(this).attr("value"));
        }
    });
}

/**
 * 金额格式化
 * @param s 原始金额
 * @param n 保留小数位数
 * @returns 格式化后的金额
 */
function currencyFormat(s, n)
{
    n = n > 0 && n <= 20 ? n : 2;
    s = parseFloat((s + "").replace(/[^\d\.-]/g, "")).toFixed(n) + "";
    var l = s.split(".")[0].split("").reverse(),
        r = s.split(".")[1];
    t = "";
    for(i = 0; i < l.length; i ++ )
    {
        t += l[i] + ((i + 1) % 3 == 0 && (i + 1) != l.length ? "," : "");
    }
    return t.split("").reverse().join("") + "." + r;
}

function checkCookies(){
    var checkName = "_client_check_flag_";
    var checkValue = "true";
    var resultValue = getCookie(checkName);

    if(checkValue == resultValue){
        //支持cookies
    }else {
        //不支持cookies
        AlertMsg("请不要禁用浏览器cookie，否则您无法体验本站，非常抱歉");
    }
}

function getCookie(cookieName) {
    var arr = document.cookie.match(new RegExp("(^| )" + cookieName + "=([^;]*)(;|$)"));
    if (arr != null){
        return unescape(arr[2]);
    }
    return null;
}


/**
 * 检测是移动设备还是pc端打开的网页
 * @returns {boolean}
 */
function isPc(){
    var userAgentInfo = navigator.userAgent;
    var Agents = ["Android", "iPhone",
        "SymbianOS", "Windows Phone",
        "iPad", "iPod"];
    var flag = true;
    for (var v = 0; v < Agents.length; v++) {
        if (userAgentInfo.indexOf(Agents[v]) > 0) {
            //手机端
            flag = false;
        }
    }
    return flag;
}