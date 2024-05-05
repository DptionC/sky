package com.sky.controller.notify;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.sky.websocket.WebSocketServer;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * 支付回调相关接口
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 支付成功回调
     *
     * @param request
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //读取数据
        String body = readData(request);
        log.info("支付成功回调：{}", body);

        //数据解密
        String plainText = decryptData(body);
        log.info("解密后的文本：{}", plainText);

        JSONObject jsonObject = JSON.parseObject(plainText);
        String outTradeNo = jsonObject.getString("out_trade_no");//商户平台订单号
        String transactionId = jsonObject.getString("transaction_id");//微信支付交易号

        log.info("商户平台订单号：{}", outTradeNo);
        log.info("微信支付交易号：{}", transactionId);

        //业务处理，修改订单状态、来单提醒
        orderService.paySuccess(outTradeNo);

        //给微信响应
        responseToWeixin(response);
    }

    /**
     * 定义读取数据方法
     * @param request
     * @return
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        //获取请求体内容,并将其转换成字符串
        BufferedReader reader = request.getReader();
        //创建非线程安全可变字符串进行字符串拼接
        StringBuilder result = new StringBuilder();
        String line = null;
        //每次读取一行
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                //长度大于0,则在尾部添加换行符,确保读取到得数据独立一行
                result.append("\n");
            }
            //将读取到得结果追加到result对象得末尾
            result.append(line);
        }
        //将最后结果以字符串结果返回
        return result.toString();
    }

    /**
     * 数据解密
     *
     * @param body
     * @return
     * @throws Exception
     */
    private String decryptData(String body) throws Exception {
        //将请求体中得数据转换成json格式
        JSONObject resultObject = JSON.parseObject(body);
        //从resultObject这个json对象获取键名为"resource"的子对象内容
        JSONObject resource = resultObject.getJSONObject("resource");
        //从resource这个子对象中获取键名为"ciphertext"加密字符串,并存储在ciphertext这个字符串变量中
        String ciphertext = resource.getString("ciphertext");
        //同上,根据不同键,获取相应的内容
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");

        //将从微信配置属性类中获取到的密钥以UTF8的格式转化成一个字节数组创建一个AesUtil工具类对象
        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        //密文解密
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);

        //将解密后的明文字符串返回
        return plainText;
    }

    /**
     * 给微信响应
     * @param response
     */
    private void responseToWeixin(HttpServletResponse response) throws Exception{
        response.setStatus(200);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("code", "SUCCESS");
        map.put("message", "SUCCESS");
        //设置响应头的格式,指定响应内容为JSON格式
        response.setHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        //获取输出流,并将map集合转换成json格式,并在转换成utf8的字节数组,通过响应的输出流写入到响应中
        response.getOutputStream().write(JSONUtils.toJSONString(map).getBytes(StandardCharsets.UTF_8));
        //将缓冲区中的内容发出
        response.flushBuffer();
    }
}
