package io.renren.modules.app.form;

import javax.validation.constraints.Min;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.Data;

/**
 * @author constantinejohn
 */
@Data
@ApiModel(value = "更新订单状态表单")
public class UpdateOrderForm {
    @ApiModelProperty(value = "订单id")
    @Min(1)
    private Integer orderId;
}


