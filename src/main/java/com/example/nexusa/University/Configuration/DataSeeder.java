package com.example.nexusa.University.Configuration;

import com.example.nexusa.University.Service.RorService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder implements ApplicationRunner {
    private final RorService rorService;

    public DataSeeder(RorService rorService) {
        this.rorService = rorService;
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Seeding universities...");
        rorService.seedUniversities();
    }
}
