package com.smartcommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling   // Required for @Scheduled in TokenBlacklistService (periodic cleanup)
public class SmartcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartcommerceApplication.class, args);
	}

}
