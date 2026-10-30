package sn.cse.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.reporting", "sn.cse.common"})
public class MsReportingApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsReportingApplication.class, args);
    }
}
