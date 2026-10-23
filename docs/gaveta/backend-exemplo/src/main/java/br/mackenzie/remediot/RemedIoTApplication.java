package br.mackenzie.remediot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@IntegrationComponentScan   // encontra o MqttGateway
@EnableScheduling           // verificação de comandos sem ack
public class RemedIoTApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemedIoTApplication.class, args);
    }
}
