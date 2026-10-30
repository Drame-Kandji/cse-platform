package sn.cse.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.audit", "sn.cse.common"})
public class MsAuditApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAuditApplication.class, args);
    }
}
