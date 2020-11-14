package io.renren.modules.app.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author constantinejohn
 */
@Data
@TableName("tb_order")
public class OrderEntity implements Serializable {
    @TableId
    private Integer id;
    /**
     * 订单的流水号
     */
    private String code;
    /**
     * 订单的id
     */
    private Integer userId;
    private BigDecimal amount;
    /**
     * 支付的类型
     */
    private Integer paymentType;
    /**
     * 支付订单Id
     */
    private String prepayId;
    /**
     * 支付的状态
     */
    private Integer status;
    private Date createTime;
}
