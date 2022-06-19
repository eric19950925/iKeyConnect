package com.sunion.ikeyconnect

import com.sunion.ikeyconnect.data.SunionTraits
import com.sunion.ikeyconnect.data.getFirmwareModelTraits
import com.sunion.ikeyconnect.domain.Interface.ILockProvider
import com.sunion.ikeyconnect.domain.Interface.Lock
import com.sunion.ikeyconnect.domain.Interface.LockInformationRepository
import com.sunion.ikeyconnect.domain.LockQRCodeParser
import com.sunion.ikeyconnect.domain.model.LockInfo
import com.sunion.ikeyconnect.lock.WifiLock
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.rx2.asFlow
import java.time.ZoneId
import java.util.*
import javax.inject.Inject


class LockProvider @Inject constructor(
    private val lockInformationRepository : LockInformationRepository,
    private val wifiLock: WifiLock
    ) : ILockProvider {
    private val locks = mutableMapOf<String, Lock>()


    override suspend fun getLockByMacAddress(macAddress: String): Lock? {
//        if (locks.containsKey(macAddress))
//            return locks[macAddress]

        val lockConnectionInfo = runCatching {
            lockInformationRepository.get(macAddress).toObservable().asFlow().single()
        }.getOrNull() ?: return null

        val lockInfo = LockInfo.from(lockConnectionInfo)

        val lock: Lock = if (getFirmwareModelTraits(lockConnectionInfo.model).contains(SunionTraits.WiFi))
            wifiLock.init(lockInfo)
        else
            wifiLock.init(lockInfo)
//            BleLock(lockInfo)

//        locks[macAddress] = lock

        return lock
    }

    override suspend fun getLockByQRCode(content: String, awsClientToken: String?): Lock? {
        val qrCodeContent = runCatching { LockQRCodeParser.parseQRCodeContent(content) }.getOrNull()
            ?: runCatching { LockQRCodeParser.parseWifiQRCodeContent(content) }.getOrNull()
            ?: return null

        val lockInfo = LockInfo.from(qrCodeContent)

        if (locks.containsKey(lockInfo.macAddress))
            return locks[lockInfo.macAddress]

        val newLock = if (getFirmwareModelTraits(lockInfo.model).contains(SunionTraits.WiFi))
            awsClientToken?.let { createWifiLock(lockInfo, it) }
        else
            wifiLock.init(lockInfo)
//            BleLock(lockInfo)

        if (newLock == null)
            return null

        locks[lockInfo.macAddress] = newLock

        return newLock
    }

    private suspend fun createWifiLock(lockInfo: LockInfo, awsClientToken: String): WifiLock? {
        if (lockInfo.serialNumber == null)
            return null

        val wifiLock = wifiLock.init(lockInfo)

        if (!wifiLock.deviceProvisionCreate(
                serialNumber = lockInfo.serialNumber,
                deviceName = lockInfo.deviceName,
                timeZone = ZoneId.systemDefault().id,
                timeZoneOffset = TimeZone.getDefault()
                    .getOffset(System.currentTimeMillis()) / 1000,
                clientToken = awsClientToken,
                model = lockInfo.model
            )
        ) return null

        return wifiLock
    }

    /**
     * check is wifi lock or ble lock
     */
    fun getLockTypeByQRCode(content: String): Boolean? {
        val qrCodeContent = runCatching { LockQRCodeParser.parseQRCodeContent(content) }.getOrNull()
            ?: runCatching { LockQRCodeParser.parseWifiQRCodeContent(content) }.getOrNull()
            ?: return null

        val lockInfo = LockInfo.from(qrCodeContent)

        return lockInfo.model.contentEquals("wifi", ignoreCase = true)
    }
}