package ${dtoPackage};
<#if extend??>
import com.xh.common.core.dto.${extend}DTO;
</#if>
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

<#if hasBigDecimal>
import java.math.BigDecimal;
</#if>
<#if hasLocalDate>
import java.time.LocalDate;
</#if>
<#if hasLocalDateTime>
import java.time.LocalDateTime;
</#if>

/**
 * ${name} DTO
 *
 * @author ${author}
 * @since ${date}
 */
@Schema(title = "${name}")
@Data
public class ${dtoName}${dtoExtendClassStr!''} {
<#list columns as field>
<#if !(field.isExtend!false)>

    @Schema(title ="${field.columnName}")
    private ${field.javaType} ${field.prop};
</#if>
</#list>
}
