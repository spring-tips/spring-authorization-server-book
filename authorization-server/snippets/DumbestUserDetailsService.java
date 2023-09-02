package bootiful.authorizationserver;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class DumbestUserDetailsService implements UserDetailsService {

    private final Map<String, UserDetails> users;

    DumbestUserDetailsService( Set <UserDetails> userDetails) {
        var map = userDetails
                .stream()
                .collect(Collectors.toMap(UserDetails::getUsername, ud -> ud));
        this.users = new ConcurrentHashMap<>(map);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Assert.hasText(username, "the username must not be null");
        var result = this.users.getOrDefault(username, null);
        if (null == result)
            throw new UsernameNotFoundException("the user %s could not be found".formatted(username));
        return result;
    }
}
