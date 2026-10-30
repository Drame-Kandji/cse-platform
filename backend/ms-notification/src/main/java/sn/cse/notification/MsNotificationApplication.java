package sn.cse.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.notification", "sn.cse.common"})
public class MsNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsNotificationApplication.class, args);
    }
}
