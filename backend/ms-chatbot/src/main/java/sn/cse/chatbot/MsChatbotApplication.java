package sn.cse.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"sn.cse.chatbot", "sn.cse.common"})
public class MsChatbotApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsChatbotApplication.class, args);
    }
}
