package org.dromara.northstar.strategy.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.strategy.api.Account;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;

import xyz.redtorch.pb.CoreField.AccountField;

class AccountTest {
	
	AccountField af = AccountField.newBuilder()
			.setAvailable(100)
			.build();

	@Test
	void testException() {
		Account account = new Account();
		
		assertThrows(IllegalStateException.class, () ->{
			account.getAccountField();
		});
		
		assertThrows(IllegalStateException.class, () -> {
			account.trylockAmount(0);
		});
		
	}
	
	@Test
	void testLock() {
		Account account = new Account();
		account.syncAmount(af);
		assertThat(account.trylockAmount(100)).isPresent();
		assertThat(account.trylockAmount(0.1)).isEmpty();
	}
	
	@Test
	void testUnlock() {
		Account account = new Account();
		account.syncAmount(af);
		Optional<UUID> lockId = account.trylockAmount(100); 
		assertThat(lockId).isPresent();
		account.unlockAmount(lockId.get());
		assertThat(account.trylockAmount(0.1)).isPresent();
	}
	
}
