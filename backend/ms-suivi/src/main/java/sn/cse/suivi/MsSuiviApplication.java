package sn.cse.suivi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.suivi", "sn.cse.common"})
public class MsSuiviApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsSuiviApplication.class, args);
    }
}
