package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.PartyEntity
import com.example.data.entity.TransactionEntity
import com.example.utils.BalanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.ShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `launch MainActivity directly`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume().visible()
        val activity = controller.get()
        assertNotNull(activity)
    }

    @Test
    fun `read string from context verifies HisabPro app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HisabPro", appName)
    }

    @Test
    fun `verify customer accounting calculation sequence`() {
        // Step 1: Customer created
        val customer = PartyEntity(
            id = 1L,
            name = "Rahul Sharma",
            phone = "9876543210",
            isSupplier = false
        )

        // Step 2: Add 1,000 Money Given
        val tx1 = TransactionEntity(partyId = 1L, type = "GAVE", amount = 1000.0)
        val bal1 = CurrencyUtils.calculateBalance(customer, listOf(tx1))
        assertEquals(1000.0, bal1.netAmount, 0.001)
        assertEquals(BalanceStatus.YOU_WILL_GET, bal1.status)

        // Step 3: Add 400 Money Received
        val tx2 = TransactionEntity(partyId = 1L, type = "GOT", amount = 400.0)
        val bal2 = CurrencyUtils.calculateBalance(customer, listOf(tx1, tx2))
        assertEquals(600.0, bal2.netAmount, 0.001)
        assertEquals(BalanceStatus.YOU_WILL_GET, bal2.status)

        // Step 4: Add 600 Money Received
        val tx3 = TransactionEntity(partyId = 1L, type = "GOT", amount = 600.0)
        val bal3 = CurrencyUtils.calculateBalance(customer, listOf(tx1, tx2, tx3))
        assertEquals(0.0, bal3.netAmount, 0.001)
        assertEquals(BalanceStatus.SETTLED, bal3.status)
    }

    @Test
    fun `verify WhatsApp reminder template message generation`() {
        val msg = ShareHelper.buildReminderMessage(
            partyName = "Rahul",
            amountFormatted = "₹1,500",
            businessName = "Shree Electronics"
        )
        assertTrue(msg.contains("Hello Rahul"))
        assertTrue(msg.contains("₹1,500"))
        assertTrue(msg.contains("pending amount"))
    }
}
