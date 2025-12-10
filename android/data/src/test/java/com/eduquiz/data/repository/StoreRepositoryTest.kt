package com.eduquiz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.db.ProfileDao
import com.eduquiz.data.db.StoreDao
import com.eduquiz.data.db.UserProfileEntity
import com.eduquiz.domain.profile.SyncState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StoreRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var storeDao: StoreDao
    private lateinit var repository: StoreRepositoryImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        profileDao = database.profileDao()
        storeDao = database.storeDao()
        repository = StoreRepositoryImpl(profileDao, storeDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun purchaseCosmetic_descuentaCoins() = runTest {
        val uid = "user1"
        val initialCoins = 100
        val cosmetic = CosmeticCatalog.COSMETICS.find { it.cost > 0 }!!
        
        // Crear perfil con coins iniciales
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test User",
                photoUrl = null,
                schoolId = null,
                classroomId = null,
                coins = initialCoins,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )

        // Comprar cosmético
        val success = repository.purchaseCosmetic(uid, cosmetic.cosmeticId)
        assertTrue(success, "La compra debería ser exitosa")

        // Verificar que los coins se descontaron
        val profile = profileDao.observeProfile(uid).firstOrNull()
        assertNotNull(profile)
        assertEquals(initialCoins - cosmetic.cost, profile.coins, "Los coins deberían descontarse correctamente")

        // Verificar que está en el inventario
        assertTrue(storeDao.hasInventoryItem(uid, cosmetic.cosmeticId), "El cosmético debería estar en el inventario")
    }

    @Test
    fun purchaseCosmetic_noPermiteCoinsNegativos() = runTest {
        val uid = "user1"
        val initialCoins = 10
        val expensiveCosmetic = CosmeticCatalog.COSMETICS.find { it.cost > initialCoins }!!
        
        // Crear perfil con pocos coins
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test User",
                photoUrl = null,
                schoolId = null,
                classroomId = null,
                coins = initialCoins,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )

        // Intentar comprar cosmético caro
        val success = repository.purchaseCosmetic(uid, expensiveCosmetic.cosmeticId)
        assertFalse(success, "La compra debería fallar por coins insuficientes")

        // Verificar que los coins no cambiaron
        val profile = profileDao.observeProfile(uid).firstOrNull()
        assertNotNull(profile)
        assertEquals(initialCoins, profile.coins, "Los coins no deberían cambiar")

        // Verificar que NO está en el inventario
        assertFalse(storeDao.hasInventoryItem(uid, expensiveCosmetic.cosmeticId), "El cosmético NO debería estar en el inventario")
    }

    @Test
    fun purchaseCosmetic_noDuplicaCompras() = runTest {
        val uid = "user1"
        val initialCoins = 200
        val cosmetic = CosmeticCatalog.COSMETICS.find { it.cost > 0 }!!
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test User",
                photoUrl = null,
                schoolId = null,
                classroomId = null,
                coins = initialCoins,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )

        // Primera compra
        val firstPurchase = repository.purchaseCosmetic(uid, cosmetic.cosmeticId)
        assertTrue(firstPurchase, "La primera compra debería ser exitosa")

        val coinsAfterFirst = profileDao.observeProfile(uid).firstOrNull()!!.coins

        // Segunda compra (intento de duplicado)
        val secondPurchase = repository.purchaseCosmetic(uid, cosmetic.cosmeticId)
        assertFalse(secondPurchase, "La segunda compra debería fallar (ya está comprado)")

        // Verificar que los coins no cambiaron en la segunda compra
        val coinsAfterSecond = profileDao.observeProfile(uid).firstOrNull()!!.coins
        assertEquals(coinsAfterFirst, coinsAfterSecond, "Los coins no deberían cambiar en la segunda compra")
    }

    @Test
    fun equipCosmetic_soloSiComprado() = runTest {
        val uid = "user1"
        val cosmetic = CosmeticCatalog.COSMETICS.find { it.cost > 0 }!!
        
        // Crear perfil
        profileDao.upsertProfile(
            UserProfileEntity(
                uid = uid,
                displayName = "Test User",
                photoUrl = null,
                schoolId = null,
                classroomId = null,
                ugelCode = null,
                coins = 0,
                xp = 0L,
                selectedCosmeticId = null,
                updatedAtLocal = System.currentTimeMillis(),
                syncState = SyncState.SYNCED
            )
        )

        // Intentar equipar sin comprar
        val equipWithoutPurchase = repository.equipCosmetic(uid, cosmetic.cosmeticId)
        assertFalse(equipWithoutPurchase, "No debería poder equipar sin comprar")

        // Comprar primero (necesitamos coins)
        profileDao.updateCoins(uid, cosmetic.cost, System.currentTimeMillis(), SyncState.SYNCED)
        val purchaseSuccess = repository.purchaseCosmetic(uid, cosmetic.cosmeticId)
        assertTrue(purchaseSuccess, "La compra debería ser exitosa")

        // Ahora equipar
        val equipAfterPurchase = repository.equipCosmetic(uid, cosmetic.cosmeticId)
        assertTrue(equipAfterPurchase, "Debería poder equipar después de comprar")

        // Verificar que está equipado
        val profile = profileDao.observeProfile(uid).firstOrNull()
        assertNotNull(profile)
        assertEquals(cosmetic.cosmeticId, profile.selectedCosmeticId, "El cosmético debería estar equipado")
    }
}

