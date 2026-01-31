package com.freetime.wallet

import java.util.Map

data class BlockchairResponse(
    val data: Map<String, AddressData>
)

data class AddressData(
    val address: Address
)

data class Address(
    val balance: Long
)
