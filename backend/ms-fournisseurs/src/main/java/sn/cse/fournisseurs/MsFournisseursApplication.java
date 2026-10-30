package sn.cse.fournisseurs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.fournisseurs", "sn.cse.common"})
public class MsFournisseursApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsFournisseursApplication.class, args);
    }
}
