package com.realtimechat.core.network.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.realtimechat.core.model.SafeLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NearbySyncManager(
    context: Context,
    private val currentUserId: String
) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val connectedEndpoints = mutableSetOf<String>()

    private val _incomingPayloads = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingPayloads: SharedFlow<ByteArray> = _incomingPayloads.asSharedFlow()

    fun startAdvertisingAndDiscovery(serviceId: String = SERVICE_ID) {
        SafeLog.d(TAG, "Starting Nearby Advertising and Discovery for $currentUserId")

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            currentUserId,
            serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            SafeLog.d(TAG, "Nearby Advertising started successfully.")
        }.addOnFailureListener { e ->
            SafeLog.e(TAG, "Nearby Advertising failed", e)
        }

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            SafeLog.d(TAG, "Nearby Discovery started successfully.")
        }.addOnFailureListener { e ->
            SafeLog.e(TAG, "Nearby Discovery failed", e)
        }
    }

    fun stopNearbySync() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        SafeLog.d(TAG, "Stopped Nearby Sync.")
    }

    fun sendPayloadToConnectedPeers(bytes: ByteArray) {
        val payload = Payload.fromBytes(bytes)
        connectedEndpoints.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            SafeLog.d(TAG, "Discovered Nearby peer endpoint: $endpointId (${info.endpointName})")
            connectionsClient.requestConnection(currentUserId, endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            SafeLog.d(TAG, "Lost Nearby peer endpoint: $endpointId")
            connectedEndpoints.remove(endpointId)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            SafeLog.d(TAG, "Connection initiated with Nearby peer $endpointId (${info.endpointName}). Accepting...")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                SafeLog.d(TAG, "Connected successfully to Nearby peer: $endpointId")
                connectedEndpoints.add(endpointId)
            } else {
                SafeLog.w(TAG, "Failed to connect to Nearby peer: $endpointId status: ${result.status}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            SafeLog.d(TAG, "Disconnected from Nearby peer: $endpointId")
            connectedEndpoints.remove(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes()
            if (bytes != null) {
                SafeLog.d(TAG, "Received ${bytes.size} bytes from Nearby peer: $endpointId")
                _incomingPayloads.tryEmit(bytes)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    companion object {
        private const val TAG = "NearbySyncManager"
        private const val SERVICE_ID = "com.realtimechat.NEARBY_SYNC"
    }
}
