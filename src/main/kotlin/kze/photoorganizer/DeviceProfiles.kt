package kze.photoorganizer

data class DeviceProfile(
        val photosUseExif: Boolean,
        val photosTimeOffsetInMins: Int,
        val videosTimeoffsetInMins: Int // TODO: Implement
)

private val devices = mapOf(
        "nexus5x" to DeviceProfile(false, 60, 120),
        "canon-s120" to DeviceProfile(false, 0, 0)
)

fun profileByName(profileName: String): DeviceProfile? {
    return devices[profileName]
}