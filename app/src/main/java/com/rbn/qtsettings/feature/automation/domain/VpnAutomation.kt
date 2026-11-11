package com.rbn.qtsettings.feature.automation.domain

/** Exact platform values retained for the existing vpn_detection_shared_state format. */
data class VpnDnsState(val mode: String, val hostname: String?)
interface VpnAutomationGateway {
    fun currentDns(): VpnDnsState
    fun savedDns(): VpnDnsState?
    fun save(state: VpnDnsState)
    fun clear()
    fun disableDns(): Boolean
    fun restoreDns(state: VpnDnsState): Boolean
}

/** Shared by foreground TileService and background service. Call on their serialized main lane. */
class VpnAutomation(private val gateway: VpnAutomationGateway) {
    fun initialize(connected: Boolean, applyNetworkPolicy: () -> Boolean) {
        if (connected && gateway.savedDns() == null) connected()
        else if (!connected && gateway.savedDns() != null) disconnected(applyNetworkPolicy)
    }
    fun connected() {
        val current = gateway.currentDns()
        if (current.mode != "off" && gateway.disableDns()) gateway.save(current)
    }
    fun disconnected(applyNetworkPolicy: () -> Boolean, applyWithoutSavedState: Boolean = false): Boolean {
        val previous = gateway.savedDns()
        if (previous == null) return applyWithoutSavedState && applyNetworkPolicy()
        val restored = applyNetworkPolicy() || gateway.restoreDns(previous)
        if (restored) gateway.clear()
        return restored
    }
    fun clear() = gateway.clear()
}
