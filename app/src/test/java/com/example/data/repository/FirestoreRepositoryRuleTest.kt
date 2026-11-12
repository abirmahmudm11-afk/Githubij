package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createChat_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser("alice@clouke.ai")
        val repository = FirestoreRepository(firestore)

        val chatId = withTimeout(10000L) {
            repository.createChat("Architecture Session")
        }
        assertNotNull(chatId)
        assertTrue(chatId.isNotEmpty())
    }

    @Test
    fun createWorkspace_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser("alice@clouke.ai")
        val repository = FirestoreRepository(firestore)

        val wsId = withTimeout(10000L) {
            repository.createWorkspace("Coltef Core", "Kotlin", "Core Engine")
        }
        assertNotNull(wsId)
        assertTrue(wsId.isNotEmpty())
    }
}
