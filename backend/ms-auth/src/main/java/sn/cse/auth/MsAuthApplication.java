package sn.cse.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.auth", "sn.cse.common"})
public class MsAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAuthApplication.class, args);
    }
}
