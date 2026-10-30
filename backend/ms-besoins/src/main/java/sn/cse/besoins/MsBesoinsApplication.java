package sn.cse.besoins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.besoins", "sn.cse.common"})
public class MsBesoinsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsBesoinsApplication.class, args);
    }
}
