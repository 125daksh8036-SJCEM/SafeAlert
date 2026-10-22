package com.example.data.repository

import com.example.data.local.EmergencyContactDao
import com.example.data.local.EmergencyContactEntity
import com.example.data.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface EmergencyContactRepository {
    val allContacts: Flow<List<EmergencyContact>>
    suspend fun getSelectedContacts(): List<EmergencyContact>
    suspend fun getContactCount(): Int
    suspend fun addContact(contact: EmergencyContact): Long
    suspend fun updateContact(contact: EmergencyContact)
    suspend fun toggleContactSelection(id: Long, isSelected: Boolean)
    suspend fun deleteContact(contact: EmergencyContact)
    suspend fun deleteContactById(id: Long)
}

class LocalEmergencyContactRepository(
    private val contactDao: EmergencyContactDao
) : EmergencyContactRepository {

    override val allContacts: Flow<List<EmergencyContact>> = contactDao.getAllContacts().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getSelectedContacts(): List<EmergencyContact> {
        return contactDao.getSelectedContacts().map { it.toDomain() }
    }

    override suspend fun getContactCount(): Int {
        return contactDao.getContactCount()
    }

    override suspend fun addContact(contact: EmergencyContact): Long {
        return contactDao.insertContact(contact.toEntity())
    }

    override suspend fun updateContact(contact: EmergencyContact) {
        contactDao.updateContact(contact.toEntity())
    }

    override suspend fun toggleContactSelection(id: Long, isSelected: Boolean) {
        contactDao.updateSelection(id, isSelected)
    }

    override suspend fun deleteContact(contact: EmergencyContact) {
        contactDao.deleteContact(contact.toEntity())
    }

    override suspend fun deleteContactById(id: Long) {
        contactDao.deleteContactById(id)
    }
}

fun EmergencyContactEntity.toDomain(): EmergencyContact = EmergencyContact(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    isSelected = isSelected
)

fun EmergencyContact.toEntity(): EmergencyContactEntity = EmergencyContactEntity(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    isSelected = isSelected
)
