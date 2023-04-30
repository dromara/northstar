package org.dromara.northstar.account;

import org.dromara.northstar.common.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountManagerTest {

    private AccountManager accountManager;
    private TradeAccount account1;
    private TradeAccount account2;
    private Identifier id1;
    private Identifier id2;

    @BeforeEach
    void setUp() {
        accountManager = new AccountManager();
        id1 = Identifier.of("account1");
        id2 = Identifier.of("account2");
        account1 = mock(TradeAccount.class);
        account2 = mock(TradeAccount.class);
        
        when(account1.accountId()).thenReturn("account1");
        when(account2.accountId()).thenReturn("account2");
    }

    @Test
    void add() {
        accountManager.add(account1);
        assertTrue(accountManager.contains(id1));

        accountManager.add(account2);
        assertTrue(accountManager.contains(id2));
    }

    @Test
    void remove() {
        accountManager.add(account1);
        accountManager.add(account2);

        accountManager.remove(id1);
        assertFalse(accountManager.contains(id1));

        accountManager.remove(id2);
        assertFalse(accountManager.contains(id2));
    }

    @Test
    void get() {
        accountManager.add(account1);
        accountManager.add(account2);

        assertEquals(account1, accountManager.get(id1));
        assertEquals(account2, accountManager.get(id2));
    }

    @Test
    void contains() {
        assertFalse(accountManager.contains(id1));
        assertFalse(accountManager.contains(id2));

        accountManager.add(account1);
        assertTrue(accountManager.contains(id1));
        assertFalse(accountManager.contains(id2));

        accountManager.add(account2);
        assertTrue(accountManager.contains(id1));
        assertTrue(accountManager.contains(id2));
    }

    @Test
    void allAccounts() {
        accountManager.add(account1);
        accountManager.add(account2);

        List<TradeAccount> accounts = accountManager.allAccounts();

        assertEquals(2, accounts.size());
        assertTrue(accounts.contains(account1));
        assertTrue(accounts.contains(account2));
    }
}

