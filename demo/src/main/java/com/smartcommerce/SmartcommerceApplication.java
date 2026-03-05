package com.smartcommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableCaching
@EnableScheduling   // Activates @Scheduled in TokenBlacklistService (expired-token eviction every 30 min)
public class SmartcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartcommerceApplication.class, args);
	}

}
