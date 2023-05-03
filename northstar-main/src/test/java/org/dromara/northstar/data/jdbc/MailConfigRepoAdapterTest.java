package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import org.dromara.northstar.common.model.MailConfigDescription;
import org.dromara.northstar.data.IMailConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class MailConfigRepoAdapterTest {

	@Autowired
    private MailConfigDescriptionRepository delegate;
	
	IMailConfigRepository repo;
	
	MailConfigDescription mailConfig = MailConfigDescription.builder()
			.emailSMTPHost("smtp.126.com")
			.emailUsername("something@126.com")
			.subscriberList(List.of("someone@qq.com"))
			.build();
	
	@BeforeEach
	void prepare() {
		repo = new MailConfigRepoAdapter(delegate);
	}
	
	@Test
	void test() {
		assertDoesNotThrow(() -> {
			repo.save(mailConfig);
		});
		
		assertThat(repo.get()).isEqualTo(mailConfig);
	}

}
