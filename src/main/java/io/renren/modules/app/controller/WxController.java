package io.renren.modules.app.controller;

import java.io.*;

import java.math.BigDecimal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.wxpay.sdk.WXPayConfig;
import io.renren.modules.app.form.UpdateOrderForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import com.github.wxpay.sdk.MyWXPayConfig;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayUtil;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.app.annotation.Login;
import io.renren.modules.app.entity.OrderEntity;
import io.renren.modules.app.entity.UserEntity;
import io.renren.modules.app.form.PayOrderForm;
import io.renren.modules.app.form.WxLoginForm;
import io.renren.modules.app.service.OrderService;
import io.renren.modules.app.service.UserService;
import io.renren.modules.app.utils.JwtUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 微信相关的Controller
 *
 * @author constantinejohn
 */
@RestController
@RequestMapping("/app/wx")
@Api("微信也为接口")
public class WxController {
    private final UserService userService;
    private final OrderService orderService;
    private final JwtUtils jwtUtils;
    private final MyWXPayConfig myWXPayConfig;
    @Value("${application.app-id}")
    private String appId;
    @Value("${application.app-secret}")
    private String appSecret;
    @Value("${application.key}")
    private String key;
    @Value("${application.mch-id}")
    private String mchId;

    public WxController(UserService userService, OrderService orderService, JwtUtils jwtUtils,
                        MyWXPayConfig myWXPayConfig) {
        this.userService = userService;
        this.orderService = orderService;
        this.jwtUtils = jwtUtils;
        this.myWXPayConfig = myWXPayConfig;
    }

    /**
     * 微信登录请求
     */
    @PostMapping("login")
    @ApiOperation("登录")
    public R login(@RequestBody WxLoginForm form) {

        // 表单校验
        ValidatorUtils.validateEntity(form);

        // 微信平台验证路径
        String url = "https://api.weixin.qq.com/sns/jscode2session";

        // 提交给微信的参数
        HashMap<String, Object> hashMap = new HashMap<>(56);

        hashMap.put("appId", appId);
        hashMap.put("appSecret", appSecret);
        hashMap.put("js_code", form.getCode());
        hashMap.put("grant_type", "authorization_code");

        // 调用微信验证请求
        String response = HttpUtil.post(url, hashMap);

        // 获取openid
        JSONObject jsonObject = JSONUtil.parseObj(response);
        String openid = jsonObject.getStr("openid");

        if ((openid == null) || (openid.length() == 0)) {
            return R.error("临时登陆凭证错误");
        }

        UserEntity user = new UserEntity();

        user.setOpenId(openid);

        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>(user);
        int count = userService.count(wrapper);

        // 没有用户则插入到数据库
        if (count == 0) {
            user.setNickname(form.getNickname());
            user.setPhoto(form.getPhoto());
            user.setType(2);
            user.setCreateTime(new Date());
            userService.save(user);
        }

        // 生成令牌字符串
        user = new UserEntity();
        user.setOpenId(openid);
        wrapper = new QueryWrapper<>(user);
        user = userService.getOne(wrapper);

        Long userId = user.getUserId();

        // 生成token
        String token = jwtUtils.generateToken(userId);
        Map<String, Object> map = new HashMap<>(96);

        map.put("token", token);
        map.put("expire", jwtUtils.getExpire());

        return R.ok(map);
    }

    /**
     * 验证客户的登陆和订单等，然后将数据发送给微信
     *
     * @param form   页面数据
     * @param header header
     * @return R
     */
    @Login
    @PostMapping(value = "/microAppPayOrder")
    @ApiOperation("小程序订单")
    public R microAppPayOrder(@RequestBody PayOrderForm form, @RequestHeader HashMap header) {
        ValidatorUtils.validateEntity(form);

        String token = header.get("token").toString();
        long userId = Long.parseLong(jwtUtils.getClaimByToken(token).getSubject());
        int orderId = form.getOrderId();
        UserEntity userEntity = new UserEntity();

        userEntity.setUserId(userId);

        QueryWrapper queryWrapper = new QueryWrapper(userEntity);
        int count = userService.count(queryWrapper);

        if (count == 0) {
            return R.error("用户不存在");
        }

        String openId = userService.getOne(queryWrapper).getOpenId();
        OrderEntity orderEntity = new OrderEntity();

        orderEntity.setId(orderId);

        // userId.intValue() 不能用，之后查看原因
        orderEntity.setUserId(Integer.parseInt(String.valueOf(userId)));
        orderEntity.setStatus(1);
        queryWrapper = new QueryWrapper(orderEntity);

        int orderCount = orderService.count(queryWrapper);

        if (orderCount == 0) {
            return R.error("该用户没有这个订单");
        }

        orderEntity = new OrderEntity();
        orderEntity.setId(orderId);
        queryWrapper = new QueryWrapper(orderEntity);
        orderEntity = orderService.getOne(queryWrapper);

        // 向微信平台发出请求，创建支付订单
        // 得倒金额（因为金额是以分为单位的，且最小金额为1元，这里进行换算）
        String amount = orderEntity.getAmount().multiply(new BigDecimal("100")).intValue() + "";

        try {
            WXPay wxPay = new WXPay(myWXPayConfig);
            HashMap<String, String> map = new HashMap<>(16);

            map.put("nonce_str", WXPayUtil.generateNonceStr());
            map.put("body", "订单备注");
            map.put("out_trade_no", orderEntity.getCode());
            map.put("total_fee", amount);
            map.put("spbill_create_ip", "127.0.0.1");
            map.put("notify_url", "https://127.0.0.1/test");
            map.put("trade_type", "JSAPI");
            map.put("openid", openId);

            // 调用微信统一下单的方法
            Map<String, String> unifiedOrder = wxPay.unifiedOrder(map);
            String prepay_id = unifiedOrder.get("prepay_id");

            // 验证订单是否成功
            if ((prepay_id == null) || "".equals(prepay_id)) {
                return R.error("支付订单创建失败");
            }

            orderEntity.setPrepayId(prepay_id);

            UpdateWrapper wrapper = new UpdateWrapper();

            wrapper.eq("id", orderEntity.getId());

            // 将支付订单id存入到数据库
            orderService.update(wrapper);

            // 生成数字签名
            map.clear();
            map.put("appId", appId);

            // new Date().getTime() 获取当前秒数替换成System.currentTimeMillis()
            String timeStamp = System.currentTimeMillis() + "";

            map.put("timeStamp", timeStamp);

            String nonceStr = WXPayUtil.generateNonceStr();

            map.put("nonceStr", nonceStr);
            map.put("package", "prepay_id = " + prepay_id);
            map.put("signType", "MD5");

            String paySign = WXPayUtil.generateSignature(map, key);

            return R.ok()
                    .put("package", "prepay_id = " + prepay_id)
                    .put("timeStamp", timeStamp)
                    .put("nonceStr", nonceStr)
                    .put("paySign", paySign);
        } catch (Exception e) {
            e.printStackTrace();

            return R.error("微信支付模块错误");
        }
    }

    /**
     * 获取支付成功的通知
     *
     * @param request  request
     * @param response response
     * @throws Exception exception
     */
    @ApiOperation("接收消息通知")
    @RequestMapping("/recieveMessage")
    public void recieveMessage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setCharacterEncoding("utf-8");

        Reader reader = request.getReader();
        BufferedReader buffer = new BufferedReader(reader);
        String line = buffer.readLine();
        StringBuffer temp = new StringBuffer();

        // 读取微信发来的xml数据
        while (line != null) {
            temp.append(line);
            line = buffer.readLine();
        }

        buffer.close();
        reader.close();

        // 解析xml数据
        Map<String, String> xmlToMap = WXPayUtil.xmlToMap(temp.toString());
        String returnCode = xmlToMap.get("return_code");
        String resultCode = xmlToMap.get("result_code");

        if ("SUCCESS".equals(resultCode) && "SUCCESS".equals(returnCode)) {
            String outTradeNo = xmlToMap.get("out_trade_no");
            UpdateWrapper wrapper = new UpdateWrapper<>();

            // 修改数据库订单支付状态¬
            wrapper.eq("code", outTradeNo);
            wrapper.set("status", 2);
            orderService.update(wrapper);
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/xml");

            // 告诉微信我收到请求了
            Writer writer = response.getWriter();
            BufferedWriter bufferedWriter = new BufferedWriter(writer);

            bufferedWriter.write("<xml>\n" + "  <return_code><![CDATA[SUCCESS]]></return_code>\n"
                    + "  <return_msg><![CDATA[OK]]></return_msg>\n" + "</xml>");
            bufferedWriter.close();
            buffer.close();
        }
    }

    @Login
    @PostMapping("/updateOrderStatus")
    @ApiOperation("更新商品订单状态")
    public R updateOrderStatus(@RequestBody UpdateOrderForm form, @RequestHeader HashMap header) {
        ValidatorUtils.validateEntity(form);
        String token = header.get("token").toString();
        int userId = Integer.parseInt(jwtUtils.getClaimByToken(token).getSubject());
        Integer orderId = form.getOrderId();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        orderEntity.setId(orderId);
        QueryWrapper wrapper = new QueryWrapper(orderEntity);

        int count = orderService.count(wrapper);
        if (count == 0) {
            return R.error("用户与订单不匹配");
        }

        OrderEntity entity = orderService.getOne(wrapper);
        String code = entity.getCode();

        HashMap<String, String> map = new HashMap<>(16);
        map.put("out_trade_no", code);
        map.put("appid", appId);
        map.put("mch_id", mchId);
        map.put("nonce_str", WXPayUtil.generateNonceStr());

        try {
            String sign = WXPayUtil.generateSignature(map, key);
            map.put("sign", sign);
            WXPay pay = new WXPay(myWXPayConfig);
            Map<String, String> orderQuery = pay.orderQuery(map);
            String returnCode = orderQuery.get("return_code");
            String resultCode = orderQuery.get("result_code");

            if ("SUCCESS".equals(resultCode) && "SUCCESS".equals(returnCode)) {
                String tradeState = orderQuery.get("trade_state");
                if ("SUCCESS".equals(tradeState)) {
                    UpdateWrapper updateWrapper = new UpdateWrapper<>();

                    // 修改数据库订单支付状态¬
                    updateWrapper.eq("code", code);
                    updateWrapper.set("status", 2);
                    orderService.update(updateWrapper);
                    return R.ok("订单状态已修改");
                } else {
                    return R.error("订单状态未修改");
                }
            }
            return R.error("订单查询状态未修改");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询支付订单失败");
        }
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
