package ${entityPackage};

<#if extend??>
import com.xh.common.core.entity.${extend};
</#if>
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
<#if hasId>
import jakarta.persistence.Id;
</#if>
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
 * ${name} Entity
 *
 * @author ${author}
 * @since ${date}
 */
@Schema(title = "${name}")
@Table(name = "${tableName}")
@Data
public class ${entityName}${entityExtendClass!''} {
<#list columns as field>
<#if !(field.isVirtual!false) && !(field.isExtend!false)>

    <#if field.primaryKey!false>
    @Id
    </#if>
    @Schema(title ="${field.remarks}")
    @Column(name = "${field.columnName}")
    private ${field.javaType} ${field.prop};
</#if>
</#list>
}
