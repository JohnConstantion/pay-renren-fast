package io.renren.modules.app.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.app.annotation.Login;
import io.renren.modules.app.entity.OrderEntity;
import io.renren.modules.app.entity.UserEntity;
import io.renren.modules.app.form.UserOrderForm;
import io.renren.modules.app.form.WxLoginForm;
import io.renren.modules.app.service.OrderService;
import io.renren.modules.app.service.UserService;
import io.renren.modules.app.utils.JwtUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.aspectj.weaver.ast.Var;
import org.omg.CORBA.OBJ_ADAPTER;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单controller
 *
 * @author constantinejohn
 */
@RestController
@RequestMapping("/app/order")
@Api("订单业务¬接口")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtils jwtUtils;

    public OrderController(OrderService orderService, JwtUtils jwtUtils) {
        this.orderService = orderService;
        this.jwtUtils = jwtUtils;
    }


    /**
     * 查询用户订单
     *
     * @param form   form
     * @param header 携带token字符串等数据
     * @return R
     */
    @Login
    @PostMapping("/searchUserOrderList")
    @ApiOperation("查询用户订单")
    public R searchUserOrderList(@RequestBody UserOrderForm form, @RequestHeader HashMap header) {
        ValidatorUtils.validateEntity(form);

        String token = header.get("token").toString();
        //反向解析token，获取用户的userId
        int userId = Integer.parseInt(jwtUtils.getClaimByToken(token).getSubject());
        Integer page = form.getPage();
        Integer length = form.getLength();
        int start = (page - 1) * length;

        HashMap<String, Object> map = new HashMap<>(20);
        map.put("userId", userId);
        map.put("start", start);
        map.put("length", length);

        ArrayList<OrderEntity> entities = orderService.searchUserOrderList(map);

        return R.ok().put("list", entities);
    }
}
