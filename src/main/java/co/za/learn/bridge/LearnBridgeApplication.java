package co.za.learn.bridge;

import co.za.learn.bridge.model.dto.ERole;
import co.za.learn.bridge.model.entity.Role;
import co.za.learn.bridge.model.entity.lookups.AppConfig;
import co.za.learn.bridge.repository.AppConfigRepository;
import co.za.learn.bridge.repository.RoleRepository;
import co.za.learn.bridge.utils.ConstantUtil;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableCaching
@EnableScheduling
@AllArgsConstructor
@SpringBootApplication
@ComponentScan({"co.za.learn.bridge"})
public class LearnBridgeApplication implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(LearnBridgeApplication.class);

  private final RoleRepository roleRepository;
  private final AppConfigRepository appConfigRepository;

  public static void main(String[] args) {
    SpringApplication.run(LearnBridgeApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    initializeRoles();
  }

  private void initializeRoles() {
    List<ERole> rolesToAdd =
        Stream.of(ERole.values()).filter(role -> !roleRepository.existsByName(role)).toList();

    if (!rolesToAdd.isEmpty()) {
      List<Role> roles = rolesToAdd.stream().map(role -> new Role(null, role)).toList();

      roleRepository.saveAll(roles);
      LOGGER.info("New roles initialized: {}", roles);
    }

    AppConfig mailUser = appConfigRepository.findByCode("MAIL_USER");
    AppConfig mailPass = appConfigRepository.findByCode("MAIL_PASS");

    if (mailUser == null || mailPass == null) {

      AppConfig defaultMailUser = new AppConfig();
      defaultMailUser.setCode("MAIL_USER");
      defaultMailUser.setDescription(ConstantUtil.MAIL_USER);
      defaultMailUser.setCreateDate(new Date());
      defaultMailUser.setLastUpdateDate(new Date());

      AppConfig defaultMailPass = new AppConfig();
      defaultMailPass.setCode("MAIL_PASS");
      defaultMailPass.setDescription(ConstantUtil.MAIL_PASS);
      defaultMailPass.setCreateDate(new Date());
      defaultMailPass.setLastUpdateDate(new Date());

      appConfigRepository.save(defaultMailUser);
      appConfigRepository.save(defaultMailPass);
    }
  }
}
