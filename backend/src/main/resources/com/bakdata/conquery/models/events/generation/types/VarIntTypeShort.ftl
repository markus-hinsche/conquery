<#macro nullValue type>Short.MAX_VALUE</#macro>
<#macro kryoSerialization type>output.writeShort(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readShort()</#macro>
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro majorTypeTransformation type>${type.minValue} + ((int)<#nested>) - (int) Short.MIN_VALUE</#macro>

<#macro unboxValue type> ((Short)<#nested>).shortValue() </#macro>