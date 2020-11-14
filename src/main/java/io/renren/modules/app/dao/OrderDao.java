package io.renren.modules.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.app.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author constantinejohn
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
    /**
     * 查询用户的订单
     *
     * @return OrderEntity
     */
    public ArrayList<OrderEntity> searchUserOrderList(HashMap map);
}
