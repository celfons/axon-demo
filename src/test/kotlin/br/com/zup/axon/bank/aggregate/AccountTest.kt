package br.com.zup.axon.bank.aggregate

import br.com.zup.axon.bank.event.AccountCloseRejectEvent
import br.com.zup.axon.bank.event.AccountClosedEvent
import br.com.zup.axon.bank.event.AccountCreatedEvent
import br.com.zup.axon.bank.event.CloseAccountCommand
import br.com.zup.axon.bank.event.CreateAccountCommand
import br.com.zup.axon.bank.event.DepositMoneyCommand
import br.com.zup.axon.bank.event.MoneyDepositRejectEvent
import br.com.zup.axon.bank.event.MoneyDepositedEvent
import br.com.zup.axon.bank.event.MoneyWithdrawRejectedEvent
import br.com.zup.axon.bank.event.MoneyWithdrawnEvent
import br.com.zup.axon.bank.event.WithdrawMoneyCommand
import org.axonframework.test.aggregate.AggregateTestFixture
import org.junit.Before
import org.junit.Test

class AccountTest {

    private lateinit var fixture: AggregateTestFixture<Account>
    private val tenant = "sample_segment"
    private val metaData = mapOf("tenant" to tenant)

    @Before
    fun setUp() {
        fixture = AggregateTestFixture(Account::class.java)
    }


    @Test
    fun `create account`() {
        fixture.givenNoPriorActivity()
                .`when`(CreateAccountCommand("1", "Darth Vader", Gender.MALE, 100), metaData)
                .expectEvents(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant))
    }

    @Test
    fun `deposit`() {
        fixture.given(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant), metaData)
                .`when`(DepositMoneyCommand("1", "tx1", 100), metaData)
                .expectEvents(MoneyDepositedEvent("1", "tx1", 100, 200, tenant))
    }

    @Test
    fun `fail deposit because account is closed`() {
        fixture.given(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant),
                      AccountClosedEvent("1"), metaData)
                .`when`(DepositMoneyCommand("1", "tx1", 100), metaData)
                .expectEvents(MoneyDepositRejectEvent("1", "tx1", 100))
    }

    @Test
    fun `withdraw valid value`() {
        fixture.given(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant), metaData)
                .`when`(WithdrawMoneyCommand("1", "tx1", 100), metaData)
                .expectEvents(MoneyWithdrawnEvent("1", "tx1", 100, 0))
    }

    @Test
    fun `withdraw invalid value`() {
        fixture.given(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant), metaData)
                .`when`(WithdrawMoneyCommand("1", "tx1", 200), metaData)
                .expectEvents(MoneyWithdrawRejectedEvent("1", "tx1", 200, 100))
    }

    @Test
    fun `fail withdraw because account is closed`() {
        fixture.given(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant),
                      AccountClosedEvent("1"), metaData)
                .`when`(WithdrawMoneyCommand("1", "tx1", 100), metaData)
                .expectEvents(MoneyWithdrawRejectedEvent("1", "tx1", 100, 100))
    }

    @Test
    fun `fail to close an account with money`() {
        fixture.given(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant), metaData)
                .`when`(CloseAccountCommand("1"))
                .expectEvents(AccountCloseRejectEvent("1", 100))
    }

    @Test
    fun `close an account with balance equals to 0`() {
        fixture.given(AccountCreatedEvent("1", "Darth Vader", Gender.MALE, 100, tenant),
                      MoneyWithdrawnEvent("1", "tx1", 100, 0)
                      , metaData)
                .`when`(CloseAccountCommand("1"))
                .expectEvents(AccountClosedEvent("1"))
    }
}