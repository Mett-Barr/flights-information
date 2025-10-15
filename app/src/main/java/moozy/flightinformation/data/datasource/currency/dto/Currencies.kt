package moozy.flightinformation.data.datasource.currency.dto


import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import moozy.flightinformation.domain.model.currency.Currencies
import moozy.flightinformation.domain.model.currency.Currency
import java.math.BigDecimal


// 讓 BigDecimal 能被 kotlinx.serialization 正常序列化（以字串表示）
object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) =
        encoder.encodeString(value.toPlainString())

    override fun deserialize(decoder: Decoder): BigDecimal =
        BigDecimal(decoder.decodeString())
}


@Serializable
data class CurrenciesDto(
    val data: Map<String, JsonPrimitive>
)

fun CurrenciesDto.toCurrencies(): Currencies {
    val currencyList: List<Currency> = data.mapNotNull {
        it.value.doubleOrNull?.let { double ->
            Currency(it.key, BigDecimal.valueOf(double))
        }
    }

    return Currencies(currencyList)
}

@Serializable
data class CodesDto(
    val data: Map<String, String> // {"USD":"US Dollar", ...}
)