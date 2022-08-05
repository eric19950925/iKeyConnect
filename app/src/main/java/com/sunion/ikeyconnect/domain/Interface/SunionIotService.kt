package com.sunion.ikeyconnect.domain.Interface

import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceListResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceProvisionTicketGetResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.DeviceUpdateResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.EventGetResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.DeviceAccessCodeGetResponse
import com.sunion.ikeyconnect.domain.model.sunion_service.payload.RegistryGetResponse
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncRequestBody
import com.sunion.ikeyconnect.domain.usecase.home.PubGetUserSyncResponseBody
import com.sunion.ikeyconnect.domain.usecase.home.GetUserSyncRequestBody


interface SunionIotService {
    suspend fun deviceProvisionCreate(
        serialNumber: String,
        deviceName: String,
        timeZone: String,
        timeZoneOffset: Int,
        clientToken: String,
        model: String,
    ): String

    suspend fun deviceProvisionTicketGet(
        Ticket: String,
        clientToken: String,
    ): DeviceProvisionTicketGetResponse

    suspend fun getDeviceList(clientToken: String): List<DeviceListResponse.Device>

    suspend fun getUserSync(request: PubGetUserSyncRequestBody): PubGetUserSyncResponseBody

    suspend fun updateUserSync(request: GetUserSyncRequestBody): PubGetUserSyncResponseBody

    suspend fun updateDeviceName(
        thingName: String,
        deviceName: String,
        clientToken: String,
    )

    suspend fun updateTimezone(thingName: String, timezone: String, clientToken: String)

    suspend fun updateAdminCode(thingName: String, adminCode: String, oldCode: String, clientToken: String)

    suspend fun delete(thingName: String, clientToken: String)

    suspend fun checkOrientation(thingName: String, clientToken: String)

    suspend fun lock(thingName: String, clientToken: String)

    suspend fun unlock(thingName: String, clientToken: String)

    suspend fun getEvent(thingName: String, timestamp: Int, clientToken: String): EventGetResponse

    suspend fun getDeviceRegistry(thingName: String, clientToken: String): RegistryGetResponse

    suspend fun updateDeviceRegistry(thingName: String, registryAttributes: RegistryGetResponse.RegistryPayload.RegistryAttributes, clientToken: String): DeviceUpdateResponse

    suspend fun updateWiFiSetting(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updateAutoLock(thingName: String, enable: Boolean, delay: Int, clientToken: String): DeviceUpdateResponse

    suspend fun updateAutoLockDelay(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updateStatusNotification(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updateVacationMode(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateKeyPressBeep(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateOfflineNotifiy(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun updatePreamble(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateSecureMode(thingName: String, enable: Boolean, clientToken: String): DeviceUpdateResponse

    suspend fun updateSyncing(thingName: String, clientToken: String): DeviceUpdateResponse

    suspend fun getAdminCode(thingName: String, clientToken: String): DeviceAccessCodeGetResponse
}