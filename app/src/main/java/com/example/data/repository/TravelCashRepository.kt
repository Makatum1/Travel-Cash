package com.example.data.repository

import com.example.data.local.TravelCashDao
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import kotlinx.coroutines.flow.Flow

class TravelCashRepository(private val dao: TravelCashDao) {

    val allTripsFlow: Flow<List<TripEntity>> = dao.getAllTripsFlow()
    val allParticipantsFlow: Flow<List<ParticipantEntity>> = dao.getAllParticipantsFlow()
    val allExpensesFlow: Flow<List<ExpenseEntity>> = dao.getAllExpensesFlow()
    val allIncomesFlow: Flow<List<IncomeEntity>> = dao.getAllIncomesFlow()

    fun getParticipantsForTrip(tripId: String): Flow<List<ParticipantEntity>> =
        dao.getParticipantsForTripFlow(tripId)

    fun getExpensesForTrip(tripId: String): Flow<List<ExpenseEntity>> =
        dao.getExpensesForTripFlow(tripId)

    fun getIncomesForTrip(tripId: String): Flow<List<IncomeEntity>> =
        dao.getIncomesForTripFlow(tripId)

    suspend fun insertTrip(trip: TripEntity) = dao.insertTrip(trip)
    suspend fun updateTrip(trip: TripEntity) = dao.updateTrip(trip)
    suspend fun deleteCompleteTrip(tripId: String) = dao.deleteCompleteTrip(tripId)

    suspend fun insertParticipant(participant: ParticipantEntity) = dao.insertParticipant(participant)
    suspend fun insertParticipants(participants: List<ParticipantEntity>) = dao.insertParticipants(participants)
    suspend fun updateParticipant(participant: ParticipantEntity) = dao.updateParticipant(participant)
    suspend fun deleteParticipant(participantId: String) = dao.deleteParticipantById(participantId)

    suspend fun insertExpense(expense: ExpenseEntity) = dao.insertExpense(expense)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>) = dao.insertExpenses(expenses)
    suspend fun updateExpense(expense: ExpenseEntity) = dao.updateExpense(expense)
    suspend fun deleteExpense(expenseId: String) = dao.deleteExpenseById(expenseId)

    suspend fun insertIncome(income: IncomeEntity) = dao.insertIncome(income)
    suspend fun insertIncomes(incomes: List<IncomeEntity>) = dao.insertIncomes(incomes)
    suspend fun updateIncome(income: IncomeEntity) = dao.updateIncome(income)
    suspend fun deleteIncome(incomeId: String) = dao.deleteIncomeById(incomeId)

    suspend fun getAllTripsOnce(): List<TripEntity> = dao.getAllTripsOnce()
    suspend fun getAllParticipantsOnce(): List<ParticipantEntity> = dao.getAllParticipantsOnce()
    suspend fun getAllExpensesOnce(): List<ExpenseEntity> = dao.getAllExpensesOnce()
    suspend fun getAllIncomesOnce(): List<IncomeEntity> = dao.getAllIncomesOnce()

    suspend fun restoreBackup(
        trips: List<TripEntity>,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        replaceAll: Boolean
    ) {
        if (replaceAll) {
            dao.replaceAllBackupData(
                trips = trips,
                participants = participants,
                expenses = expenses,
                incomes = incomes
            )
        } else {
            val existingTrips = dao.getAllTripsOnce().associateBy { it.id }
            val existingParticipants = dao.getAllParticipantsOnce()

            val participantIdRemap = mutableMapOf<String, String>()
            val participantsToInsert = mutableListOf<ParticipantEntity>()

            participants.forEach { incomingPart ->
                val duplicateInSameTrip = existingParticipants.find {
                    it.tripId == incomingPart.tripId &&
                            it.name.trim().equals(incomingPart.name.trim(), ignoreCase = true)
                }
                if (duplicateInSameTrip != null) {
                    participantIdRemap[incomingPart.id] = duplicateInSameTrip.id
                    if (!incomingPart.photoUri.isNullOrBlank() && duplicateInSameTrip.photoUri.isNullOrBlank()) {
                        participantsToInsert.add(duplicateInSameTrip.copy(photoUri = incomingPart.photoUri))
                    }
                } else {
                    participantIdRemap[incomingPart.id] = incomingPart.id
                    participantsToInsert.add(incomingPart)
                }
            }

            val mergedTrips = trips.map { incomingTrip ->
                val existing = existingTrips[incomingTrip.id]
                if (existing != null && incomingTrip.headerPhotoUri.isNullOrBlank() && !existing.headerPhotoUri.isNullOrBlank()) {
                    incomingTrip.copy(headerPhotoUri = existing.headerPhotoUri)
                } else {
                    incomingTrip
                }
            }

            val remappedExpenses = expenses.map { exp ->
                val newPaidBy = participantIdRemap[exp.paidById] ?: exp.paidById
                val newSplits = exp.splits.map { split ->
                    split.copy(participantId = participantIdRemap[split.participantId] ?: split.participantId)
                }
                exp.copy(paidById = newPaidBy, splits = newSplits)
            }

            val remappedIncomes = incomes.map { inc ->
                val newContribs = inc.contributions.map { c ->
                    c.copy(participantId = participantIdRemap[c.participantId] ?: c.participantId)
                }
                inc.copy(contributions = newContribs)
            }

            dao.upsertBackupData(
                trips = mergedTrips,
                participants = participantsToInsert,
                expenses = remappedExpenses,
                incomes = remappedIncomes
            )
        }
    }

    /**
     * Limpia cualquier viaje de demostración anterior para asegurar que la app
     * arranque completamente limpia la primera vez, sin viajes preinstalados.
     */
    suspend fun ensureSeedDataIfEmpty(): String? {
        val existingTrips = dao.getAllTripsOnce()
        // Si existe el viaje de demostración antiguo, eliminarlo en cascada para arrancar limpios
        val demoTrip = existingTrips.find { it.id == "trip-japon-2026" }
        if (demoTrip != null) {
            dao.deleteCompleteTrip(demoTrip.id)
        }
        val remainingTrips = dao.getAllTripsOnce()
        return remainingTrips.firstOrNull()?.id
    }
}
