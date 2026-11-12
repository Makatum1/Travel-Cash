package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelCashDao {

    // Trips
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTripsFlow(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    suspend fun getAllTripsOnce(): List<TripEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: String)

    // Participants
    @Query("SELECT * FROM participants WHERE tripId = :tripId")
    fun getParticipantsForTripFlow(tripId: String): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants")
    fun getAllParticipantsFlow(): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants")
    suspend fun getAllParticipantsOnce(): List<ParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM participants WHERE id = :participantId")
    suspend fun deleteParticipantById(participantId: String)

    @Query("DELETE FROM participants WHERE tripId = :tripId")
    suspend fun deleteParticipantsByTripId(tripId: String)

    // Expenses
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC, createdAt DESC")
    fun getExpensesForTripFlow(tripId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteExpensesByTripId(tripId: String)

    // Incomes (Bote común)
    @Query("SELECT * FROM incomes WHERE tripId = :tripId ORDER BY date DESC")
    fun getIncomesForTripFlow(tripId: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomesFlow(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    suspend fun getAllIncomesOnce(): List<IncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<IncomeEntity>)

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :incomeId")
    suspend fun deleteIncomeById(incomeId: String)

    @Query("DELETE FROM incomes WHERE tripId = :tripId")
    suspend fun deleteIncomesByTripId(tripId: String)

    @Transaction
    suspend fun deleteCompleteTrip(tripId: String) {
        deleteExpensesByTripId(tripId)
        deleteIncomesByTripId(tripId)
        deleteParticipantsByTripId(tripId)
        deleteTripById(tripId)
    }

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM incomes")
    suspend fun clearAllIncomes()

    @Query("DELETE FROM participants")
    suspend fun clearAllParticipants()

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()

    @Transaction
    suspend fun clearEverything() {
        clearAllExpenses()
        clearAllIncomes()
        clearAllParticipants()
        clearAllTrips()
    }

    @Transaction
    suspend fun replaceAllBackupData(
        trips: List<TripEntity>,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ) {
        clearEverything()
        insertTrips(trips)
        insertParticipants(participants)
        insertExpenses(expenses)
        insertIncomes(incomes)
    }

    @Transaction
    suspend fun upsertBackupData(
        trips: List<TripEntity>,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ) {
        insertTrips(trips)
        insertParticipants(participants)
        insertExpenses(expenses)
        insertIncomes(incomes)
    }
}
