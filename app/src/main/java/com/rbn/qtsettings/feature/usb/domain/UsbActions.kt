package com.rbn.qtsettings.feature.usb.domain

/** Shared execution policy for UI, tile, quick actions, shortcuts, and restoration. */
class UsbActions(private val gateway: UsbSystemGateway) {
    fun read() = gateway.read()
    fun hasPermission() = gateway.hasPermission()
    fun generation() = gateway.generation()

    fun set(enabled: Boolean, toggleDeveloperOptions: Boolean, toggleWireless: Boolean): UsbActionResult =
        synchronized(lock) {
            if (!gateway.hasPermission()) return@synchronized UsbActionResult.PermissionMissing
            val before = runCatching { gateway.read() }.getOrElse { return@synchronized UsbActionResult.Failed }
            if (!toggleDeveloperOptions && !before.developerOptions) {
                return@synchronized UsbActionResult.DeveloperOptionsDisabled
            }
            transact(before, before.copy(
                usb = enabled,
                developerOptions = if (toggleDeveloperOptions) enabled else before.developerOptions,
                wireless = if (toggleWireless) enabled else before.wireless
            ), toggleDeveloperOptions, toggleWireless)
        }

    data class CycleResult(val result: UsbActionResult, val previous: UsbSystemState? = null, val generation: Long = 0)

    fun cycle(settings: UsbSettings): CycleResult = synchronized(lock) {
        if (!gateway.hasPermission()) return@synchronized CycleResult(UsbActionResult.PermissionMissing)
        val before = runCatching { gateway.read() }.getOrElse { return@synchronized CycleResult(UsbActionResult.Failed) }
        if (!settings.alsoHideDevOptions && !before.developerOptions) {
            return@synchronized CycleResult(UsbActionResult.DeveloperOptionsDisabled)
        }
        val choices = buildList {
            if (settings.toggleEnable) add(true)
            if (settings.toggleDisable) add(false)
        }
        if (choices.isEmpty()) return@synchronized CycleResult(UsbActionResult.NoCycleStates)
        val effective = before.usb && (!settings.alsoHideDevOptions || before.developerOptions)
        val next = choices[(choices.indexOf(effective) + 1) % choices.size]
        CycleResult(set(next, settings.alsoHideDevOptions, settings.alsoDisableWirelessDebugging), before, gateway.generation())
    }

    /** A later successful direct action always wins over a pending timer. */
    fun restore(previous: UsbSystemState, expectedGeneration: Long, settings: UsbSettings): UsbActionResult? =
        synchronized(lock) {
            if (gateway.generation() != expectedGeneration) return@synchronized null
            if (!gateway.hasPermission()) return@synchronized UsbActionResult.PermissionMissing
            val before = runCatching { gateway.read() }.getOrElse { return@synchronized UsbActionResult.Failed }
            if (!settings.alsoHideDevOptions && !before.developerOptions) {
                return@synchronized UsbActionResult.DeveloperOptionsDisabled
            }
            val target = previous.copy(
                developerOptions = if (settings.alsoHideDevOptions) previous.usb || previous.developerOptions else before.developerOptions,
                wireless = if (settings.alsoDisableWirelessDebugging) previous.wireless else before.wireless
            )
            transact(before, target, settings.alsoHideDevOptions, settings.alsoDisableWirelessDebugging)
        }

    private fun transact(before: UsbSystemState, target: UsbSystemState, dev: Boolean, wireless: Boolean): UsbActionResult {
        val success = runCatching {
            (!dev || !target.developerOptions || gateway.write(UsbSetting.DeveloperOptions, true)) &&
                gateway.write(UsbSetting.Usb, target.usb) &&
                (!wireless || gateway.write(UsbSetting.Wireless, target.wireless)) &&
                (!dev || target.developerOptions || gateway.write(UsbSetting.DeveloperOptions, false))
        }.getOrDefault(false)
        if (success) {
            gateway.invalidateRevert()
            return UsbActionResult.Success
        }
        // Attempt every rollback write even if one fails; never report a partial write as success.
        val restored = mutableListOf<Boolean>()
        fun rollback(setting: UsbSetting, enabled: Boolean) {
            restored += runCatching { gateway.write(setting, enabled) }.getOrDefault(false)
        }
        if (dev && before.developerOptions) rollback(UsbSetting.DeveloperOptions, true)
        rollback(UsbSetting.Usb, before.usb)
        if (wireless) rollback(UsbSetting.Wireless, before.wireless)
        if (dev) rollback(UsbSetting.DeveloperOptions, before.developerOptions)
        return if (restored.all { it }) UsbActionResult.Failed else {
            gateway.invalidateRevert()
            UsbActionResult.PartialFailure
        }
    }

    private companion object { val lock = Any() }
}
