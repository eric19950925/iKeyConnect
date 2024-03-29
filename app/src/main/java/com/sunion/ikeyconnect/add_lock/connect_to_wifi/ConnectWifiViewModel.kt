package com.sunion.ikeyconnect.add_lock.connect_to_wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.ikeyconnect.LockProvider
import com.sunion.ikeyconnect.add_lock.ProvisionDomain
import com.sunion.ikeyconnect.domain.blelock.BluetoothConnectState
import com.sunion.ikeyconnect.domain.command.WifiConnectState
import com.sunion.ikeyconnect.domain.usecase.account.GetClientTokenUseCase
import com.sunion.ikeyconnect.domain.usecase.account.GetUuidUseCase
import com.sunion.ikeyconnect.lock.WifiLock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConnectWifiViewModel @Inject constructor(
    private val getClientTokenUseCase: GetClientTokenUseCase,
    private val provisionDomain: ProvisionDomain,
    private val getUuid: GetUuidUseCase,
    private val lockProvider: LockProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectWifiUiState())
    val uiState: StateFlow<ConnectWifiUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<ConnectWifiUiEvent>()
    val uiEvent: SharedFlow<ConnectWifiUiEvent> = _uiEvent

    private var _macAddress: String? = null
    val macAddress: String?
        get() = _macAddress

    private var _ssid: String? = null
    val ssid: String?
        get() = _ssid

    private var lock: WifiLock? = null

    private var isConnectingToWifi = false

    private var isCollectingConnectToWifiState = false

    fun init(macAddress: String, ssid: String) {
        _macAddress = macAddress
        _ssid = ssid

        viewModelScope.launch(Dispatchers.IO) {
            lock = lockProvider.getLockByMacAddress(macAddress) as WifiLock?
            collectBleConnectionState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        lock?.disconnect()
    }

    fun setWifiPassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun connectToWifi() {
        val wifiLock = lock ?: return
        if (!isCollectingConnectToWifiState)
            wifiLock
                .collectConnectToWifiState()
                .flowOn(Dispatchers.IO)
                .onStart { isCollectingConnectToWifiState = true }
                .onCompletion { isCollectingConnectToWifiState = false }
                .onEach { wifiConnectState ->
                    _uiState.update {
                        it.copy(
                            progressMessage =
                            when (wifiConnectState) {
                                WifiConnectState.ConnectWifiSuccess -> "Connect to iot..."
                                WifiConnectState.ConnectWifiFail -> "Connect wifi fail."
                                WifiConnectState.ConnectAwsSuccess -> "Connect to cloud..."
                                WifiConnectState.ConnectCloudSuccess -> "Connected cloud."
                                WifiConnectState.Failed -> "Unknown error"
                            }
                        )
                    }

                    if (wifiConnectState == WifiConnectState.ConnectCloudSuccess) {
                        isConnectingToWifi = false
                        wifiLock
                            .getAndSaveThingName(getClientTokenUseCase())//todo not necessary
                            .flowOn(Dispatchers.IO)
                            .onEach {
                                _uiEvent.emit(ConnectWifiUiEvent.ConnectSuccess)
                                _uiState.update { it.copy(isProgress = false) }
                                val gotInfo = provisionDomain.getInfo(clientToken = getUuid.invoke())
                                Timber.d(gotInfo.toString())
                            }
                            .catch { Timber.e(it) }
                            .launchIn(viewModelScope)
                    }
                    else if (wifiConnectState == WifiConnectState.ConnectWifiFail) {
                        viewModelScope.launch { _uiEvent.emit(ConnectWifiUiEvent.ResetWifi) }
                    }
                }
                .catch {
                    Timber.e(it)
                    if (it.message?.contains("Disconnected") == false)
                        _uiState.update { s -> s.copy(errorMessage = "連線失敗", isProgress = false) }
                }
                .launchIn(viewModelScope)

        flow { emit(wifiLock.connectLockToWifi(ssid!!, uiState.value.password)) }
            .flowOn(Dispatchers.IO)
            .onStart {
                isConnectingToWifi = true
                _uiState.update { it.copy(isProgress = true) }
            }
            .catch {
                Timber.e(it)
                _uiState.update { s -> s.copy(errorMessage = "連線失敗") }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.IO) {
            delay(120_000)
            if (isConnectingToWifi)
                viewModelScope.launch { _uiEvent.emit(ConnectWifiUiEvent.ConnectFailed) }
        }
    }

    private fun collectBleConnectionState() {
        val wifiLock = lock ?: return
        wifiLock.connectionState2
            .onEach { state ->
                Timber.d(state.toString())
                when (state) {
                    BluetoothConnectState.DISCONNECTED -> {
//                        reconnectBle()
                        _uiEvent.emit(ConnectWifiUiEvent.BleDisconnected)
                    }
                    BluetoothConnectState.CONNECTED -> connectToWifi()
                    BluetoothConnectState.CONNECTING -> {
                        _uiEvent.emit(ConnectWifiUiEvent.BleConnecting)
                    }
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun reconnectBle() {
        lock?.connect()
//        viewModelScope.launch(Dispatchers.IO) {
//            delay(30_000)
//            if (lock?.isConnected() != true) {
//                _uiState.update { it.copy(showDisconnect = true) }
//                lock?.disconnect()
//                lock?.delete(getClientTokenUseCase())
//            }
//        }
    }
}

data class ConnectWifiUiState(
    val password: String = "",
    val errorMessage: String = "",
    val isProgress: Boolean = false,
    val progressMessage: String = "Connect to wifi...",
    val showDisconnect: Boolean = false,
)

sealed class ConnectWifiUiEvent {
    object ConnectSuccess : ConnectWifiUiEvent()
    object ConnectFailed : ConnectWifiUiEvent()
    object BleDisconnected : ConnectWifiUiEvent()
    object BleConnecting : ConnectWifiUiEvent()
    object ResetWifi : ConnectWifiUiEvent()
}