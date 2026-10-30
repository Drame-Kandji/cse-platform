package sn.cse.achats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.achats", "sn.cse.common"})
public class MsAchatsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAchatsApplication.class, args);
    }
}
