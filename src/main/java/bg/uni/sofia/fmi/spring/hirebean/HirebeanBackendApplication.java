package bg.uni.sofia.fmi.spring.hirebean;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HirebeanBackendApplication {

    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries()
                .forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(HirebeanBackendApplication.class, args);
    }

}
