package bg.uni.sofia.fmi.spring.hirebean.config;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.Role;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// CommandLineRunner се изпълнява веднъж при стартиране на приложението.
// Гарантира, че ролите CANDIDATE, EMPLOYER, ADMIN съществуват в базата.
// Без тях register() ще хвърли грешка
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        for (RoleType roleType : RoleType.values()) {
            if(roleRepository.findByName(roleType).isEmpty()) {
                roleRepository.save(new Role(null, roleType));
                log.info("Added role: {}", roleType);
            }

        }
    }
}
