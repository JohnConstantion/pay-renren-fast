package io.renren.modules.app.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * @author constantinejohn
 */
@Data
@ApiModel(value = "订单付款的表单")
public class PayOrderForm {
    @ApiModelProperty(value = "订单id")
    @Min(1)
    private Integer orderId;
}



