import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.reflection.Parameterized
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import java.io.IOException
import java.math.BigInteger


data class TokenInfo(
    var id: BigInteger,
    var tokenUri: String
) : DynamicStruct() {
    override fun getValue(): List<Type<*>> = listOf(
        Uint256(id),
        Utf8String(tokenUri)
    )
    constructor(
        id: Uint256,
        tokenUri: Utf8String
    ) : this(id.value, tokenUri.value)
}


data class Result(
    var collection: String,
    var name: String,
    var tokens: List<TokenInfo>
) : DynamicStruct(
    Address(collection),
    Utf8String(name),
    DynamicArray(TokenInfo::class.java, tokens)
) {
    constructor(
       collection: Address,
       name: Utf8String,
       @Parameterized(type = TokenInfo::class) tokens: DynamicArray<TokenInfo>
    ) : this(
    collection.value,
        name.value,
        tokens.value as List<TokenInfo>
    )
}

class SearchContract(
    private val web3j: Web3j,
    private val contractAddress: String
) {
    fun findByOwner(
        erc721Addresses: List<String>,
        ownerAddress: String,
        limit: BigInteger
    ): List<Result> {
        val function = Function(
            "findByOwner",
            listOf(
                DynamicArray(
                    Address::class.java,
                    erc721Addresses.map { Address(it) }
                ),
                Address(ownerAddress),
                Uint256(limit)
            ),
            listOf(object : TypeReference<DynamicArray<Result>>() {

            })
        )

        val encodedFunction = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                null,
                contractAddress,
                encodedFunction
            ),
            DefaultBlockParameterName.LATEST
        ).send()

        if (response.hasError()) {
            throw IOException("Contract call failed: ${response.error.message}")
        }

        try {
            println("Raw hex data: ${response.result}")
            val dataWithoutPrefix = response.result.removePrefix("0x")
            println("Data length (bytes): ${dataWithoutPrefix.length / 2}")

            val decoded = FunctionReturnDecoder.decode(
                response.result,
                function.outputParameters
            )

            println("Decoded data: $decoded")

            if (decoded.isEmpty()) {
                println("No data decoded")
                return emptyList()
            }

            return when (val result = decoded[0]) {
                is DynamicArray<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (result.value as List<Result>).also {
                        println("Number of results: ${it.size}")
                        it.forEach { r ->
                            println("Collection: ${r.collection}")
                            println("Name: ${r.name}")
                            println("Tokens: ${r.tokens.size}")
                        }
                    }
                }
                else -> {
                    println("Unexpected response type: ${result?.javaClass}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            println("Decoding error: ${e.message}")
            println("Stack trace:")
            e.printStackTrace()
            return emptyList()
        }
    }
}

fun main(args: Array<String>) {
    val web3j = Web3j.build(HttpService("https://porcini.rootnet.app/archive"))
    val contract = SearchContract(web3j, "0x130Db38980De698796F01873C4a28B3581428422")
    val erc721Addresses = listOf(
        "0xfc3De4990a8EBe9C8dEbd7C826936Eb62Ef457B4",
        "0xc6851Cd880B742163B09377Ee4092Bcd7e2266b4"
    )
    val ownerAddress = "0x81f85e63Ce049a6f72f78C4A60b8186e04EbC215"
    val limit = BigInteger.valueOf(100)
    // findByOwner 호출 및 결과 출력
    try {
        println("소유자 주소: $ownerAddress")
        println("ERC721 주소들: $erc721Addresses")
        println("제한: $limit")

        val results = contract.findByOwner(erc721Addresses, ownerAddress, limit)
        println("결과 개수: ${results.size}")

        results.forEachIndexed { index, result ->
            println("\n결과 #${index + 1}")
            println("Collection: ${result}")


        }
    } catch (e: Exception) {
        println("오류 발생: ${e.message}")
        e.printStackTrace()
    } finally {
        web3j.shutdown()
    }
}

