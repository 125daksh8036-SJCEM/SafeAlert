package com.example.data.repository

import com.example.data.local.AlertDao
import com.example.data.local.AlertEntity
import com.example.data.model.Alert
import com.example.data.model.AlertSource
import com.example.data.model.AlertStatus
import com.example.data.model.AlertType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AlertRepository {
    val allAlerts: Flow<List<Alert>>
    val latestAlert: Flow<Alert?>
    fun getAlertsBySource(source: AlertSource): Flow<List<Alert>>
    suspend fun saveAlert(alert: Alert): Long
    suspend fun updateAlertStatus(id: Long, status: AlertStatus)
    suspend fun deleteAlert(id: Long)
    suspend fun clearHistory()
}

class LocalAlertRepository(private val alertDao: AlertDao) : AlertRepository {

    override val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts().map { list ->
        list.map { it.toDomain() }
    }

    override val latestAlert: Flow<Alert?> = alertDao.getLatestAlert().map { entity ->
        entity?.toDomain()
    }

    override fun getAlertsBySource(source: AlertSource): Flow<List<Alert>> =
        alertDao.getAlertsBySource(source.name).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun saveAlert(alert: Alert): Long {
        return alertDao.insertAlert(alert.toEntity())
    }

    override suspend fun updateAlertStatus(id: Long, status: AlertStatus) {
        alertDao.updateStatus(id, status.name)
    }

    override suspend fun deleteAlert(id: Long) {
        alertDao.deleteAlertById(id)
    }

    override suspend fun clearHistory() {
        alertDao.clearAllAlerts()
    }
}

// Domain - Entity Mappings
fun AlertEntity.toDomain(): Alert {
    val parsedType = try {
        AlertType.valueOf(this.alertType)
    } catch (_: Exception) {
        AlertType.SOS
    }
    val parsedSource = try {
        AlertSource.valueOf(this.source)
    } catch (_: Exception) {
        AlertSource.HARDWARE
    }
    val parsedStatus = try {
        AlertStatus.valueOf(this.status)
    } catch (_: Exception) {
        AlertStatus.ACTIVE
    }

    return Alert(
        id = this.id,
        type = parsedType,
        timestamp = this.timestamp,
        deviceName = this.deviceName,
        deviceId = this.deviceId,
        source = parsedSource,
        status = parsedStatus,
        locationDescription = this.locationDescription ?: "Current Location: Campus Quad, Science Building"
    )
}

fun Alert.toEntity(): AlertEntity {
    return AlertEntity(
        id = this.id,
        alertType = this.type.name,
        timestamp = this.timestamp,
        deviceName = this.deviceName,
        deviceId = this.deviceId,
        source = this.source.name,
        status = this.status.name,
        locationDescription = this.locationDescription
    )
}
