package com.ibrah.parkasistani.car

data class BtDevice(val address: String, val name: String?)

object CarDeviceMatcher {
    const val DEFAULT_CAR_NAME = "Kensa"

    fun isCar(address: String, selected: Set<String>): Boolean =
        selected.any { it.equals(address, ignoreCase = true) }

    fun defaultSelection(bonded: List<BtDevice>): Set<String> =
        bonded.filter { it.name == DEFAULT_CAR_NAME }.map { it.address }.toSet()
}
