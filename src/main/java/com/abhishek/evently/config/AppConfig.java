package com.abhishek.evently.config;

import com.google.zxing.qrcode.QRCodeWriter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig
{

    @Bean
    public ModelMapper modelMapper()
    {
        return new ModelMapper();
    }

    @Bean
    public QRCodeWriter qrCodeWriter()
    {
        return new QRCodeWriter();
    }

    @Bean
    public OpenAPI swaggerConfiguration()
    {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080/api/v1");
        devServer.setDescription("Server URL for Development environment");

        Contact contact = new Contact();
        contact.setEmail("test@gmail.com");
        contact.setName("Ram");

        Info info = new Info()
                .title("Evently - An Event & Ticket Managing Platform")
                .version("1.0")
                .contact(contact)
                .description("API Documentation")
                .termsOfService("None as of now");

        return new OpenAPI().info(info).servers(List.of(devServer));
    }
}
