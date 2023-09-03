package bootiful.authorizationserver;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.Set;

@Configuration
@ImportRuntimeHints(AotConfiguration.Hints.class)
class AotConfiguration {

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

            for (var type : Set.of(
                    "org.springframework.security.core.authority.SimpleGrantedAuthority" ,
                    "org.springframework.security.authentication.UsernamePasswordAuthenticationToken",
                    "org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat"))
                hints.serialization().registerType(TypeReference.of(type));




            for (var type : Set.of(
                    "org.springframework.security.jackson2.CoreJackson2Module",
                    "org.springframework.security.cas.jackson2.CasJackson2Module",
                    "org.springframework.security.jackson2.UsernamePasswordAuthenticationTokenDeserializer" ,
                    "org.springframework.security.web.jackson2.WebJackson2Module",
                    "org.springframework.security.web.server.jackson2.WebServerJackson2Module" ,
                    "org.springframework.security.oauth2.server.authorization.jackson2.DurationMixin" ,
                    "org.springframework.security.oauth2.server.authorization.jackson2.HashSetMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.JwsAlgorithmMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationRequestDeserializer",
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationRequestMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module",
                    "org.springframework.security.oauth2.server.authorization.jackson2.UnmodifiableMapMixin",
                    "org.springframework.security.authentication.UsernamePasswordAuthenticationToken" ,
                    "org.springframework.security.core.userdetails.User" ,
                    "org.springframework.security.web.authentication.WebAuthenticationDetails" ,
                    "org.springframework.security.core.authority.SimpleGrantedAuthority" ,
                    "org.springframework.security.jackson2.UnmodifiableMapMixin" ,
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2TokenFormatMixin"  ,
                    "java.time.Duration" ,
                    "org.springframework.security.oauth2.jose.jws.SignatureAlgorithm" ,
                    "java.util.Collections$UnmodifiableMap",
                    "org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat",
                    "org.springframework.security.oauth2.server.authorization.jackson2.UnmodifiableMapDeserializer"))
                hints.reflection().registerType(TypeReference.of(type), MemberCategory.values());

            for (var folder : Set.of("data", "schema"))
                hints.resources().registerPattern("sql/" + folder + "/*sql");
        }
    }
}
