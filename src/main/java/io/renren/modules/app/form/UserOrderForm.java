package io.renren.modules.app.form;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.Data;

/**
 * @author constantinejohn
 */
@Data
@ApiModel(value = "查询用户订单的表单")
public class UserOrderForm {

    @ApiModelProperty(value = "页数")
    @NotNull(message = "页数不可为空")
    @Min(1)
    
    private Integer page;
    @ApiModelProperty(value = "每页记录数")
    @NotNull(message = "每页记录数不能为空")
    @Range(min = 1, max = 50)
    private Integer length;
}


//~ Formatted by Jindent --- http://www.jindent.com
