package bootiful.authorizationserver;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
class UsersConfiguration {

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    ApplicationRunner usersRunner(UserDetailsManager userDetailsManager) {
        return args -> {
            var users = Map.of("jlong", "password", "rwinch", "p@ssw0rd");
            users.forEach((username, password) -> {
                if (!userDetailsManager.userExists(username)) {
                    var user = User.withUsername(username).roles("USER").password(password).build();
                    userDetailsManager.createUser(user);
                }
            });
        };
    }
}
