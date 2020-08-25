<#macro nullValue type>Byte.MAX_VALUE</#macro>
<#macro kryoSerialization type>output.writeByte(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readByte()</#macro>
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro majorTypeTransformation type> ${type.minValue} + (int) <#nested> - (int) Byte.MIN_VALUE</#macro>

<#macro unboxValue type> ((Byte)<#nested>).byteValue() </#macro>