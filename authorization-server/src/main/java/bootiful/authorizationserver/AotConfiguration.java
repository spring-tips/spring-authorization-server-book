package bootiful.authorizationserver;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.security.jackson2.SecurityJackson2Modules;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@ImportRuntimeHints(AotConfiguration.Hints.class)
class AotConfiguration {

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

            for (var type : List.of(
                    "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType",
                    "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest",
                    "org.springframework.security.oauth2.server.authorization.OAuth2Authorization",
                    "org.springframework.security.core.authority.SimpleGrantedAuthority",
                    "org.springframework.security.oauth2.core.AuthorizationGrantType",
                    "org.springframework.security.authentication.UsernamePasswordAuthenticationToken",
                    "org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat",
                    "org.springframework.security.oauth2.core.AuthorizationGrantType",
                    "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest",
                    "org.springframework.security.web.jackson2.CookieMixin",
                    "org.springframework.security.web.jackson2.SavedCookieMixin",
                    "org.springframework.security.web.jackson2.DefaultSavedRequestMixin",
                    "org.springframework.security.web.jackson2.WebAuthenticationDetailsMixin",
                    "org.springframework.security.web.savedrequest.DefaultSavedRequest",
                    "org.springframework.security.web.savedrequest.SavedCookie",
                    "java.util.ArrayList",
                    "java.util.Collections$EmptyList",
                    "java.util.Collections$EmptyMap",
                    "java.util.Collections$UnmodifiableRandomAccessList",
                    "java.util.Collections$SingletonList",
                    "java.util.Collections$UnmodifiableSet",
                    "java.util.Date",
                    "java.time.Instant",
                    "java.net.URL",
                    "java.util.TreeMap",
                    "java.util.HashMap",
                    "java.util.LinkedHashMap",
                    "java.util.Arrays$ArrayList",
                    "java.util.List",
                    "org.springframework.security.core.context.SecurityContextImpl",
                    "org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module",
                    "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
                    "org.springframework.security.ldap.jackson2.LdapJackson2Module",
                    "org.springframework.security.saml2.jackson2.Saml2Jackson2Module",
                    "jakarta.servlet.http.Cookie",
                    "org.springframework.security.oauth2.client.OAuth2AuthorizedClient",
                    "com.fasterxml.jackson.databind.Module",
                    "org.springframework.security.web.jackson2.WebServletJackson2Module",
                    "org.springframework.security.oauth2.server.authorization.OAuth2Authorization",
                    "org.springframework.security.jackson2.AnonymousAuthenticationTokenMixin",
                    "org.springframework.security.jackson2.BadCredentialsExceptionMixin",
                    "org.springframework.security.jackson2.CoreJackson2Module",
                    "org.springframework.security.jackson2.RememberMeAuthenticationTokenMixin",
                    "org.springframework.security.jackson2.SecurityJackson2Modules",
                    "org.springframework.security.jackson2.SimpleGrantedAuthorityMixin",
                    "org.springframework.security.jackson2.UnmodifiableListDeserializer",
                    "org.springframework.security.jackson2.UnmodifiableListMixin",
                    "org.springframework.security.jackson2.UnmodifiableMapDeserializer",
                    "org.springframework.security.jackson2.UnmodifiableSetDeserializer",
                    "org.springframework.security.jackson2.UnmodifiableSetMixin",
                    "org.springframework.security.jackson2.UserDeserializer",
                    "org.springframework.security.jackson2.UserMixin",
                    "org.springframework.security.jackson2.UsernamePasswordAuthenticationTokenMixin",
                    "org.springframework.security.cas.jackson2.CasJackson2Module",
                    "org.springframework.security.jackson2.UsernamePasswordAuthenticationTokenDeserializer",
                    "org.springframework.security.web.jackson2.WebJackson2Module",
                    "org.springframework.security.web.server.jackson2.WebServerJackson2Module",
                    "org.springframework.security.oauth2.server.authorization.jackson2.DurationMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.HashSetMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.JwsAlgorithmMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationRequestDeserializer",
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationRequestMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module",
                    "org.springframework.security.oauth2.server.authorization.jackson2.UnmodifiableMapMixin",
                    "org.springframework.security.authentication.UsernamePasswordAuthenticationToken",
                    "org.springframework.security.core.userdetails.User",
                    "org.springframework.security.web.authentication.WebAuthenticationDetails",
                    "org.springframework.security.core.authority.SimpleGrantedAuthority",
                    "org.springframework.security.jackson2.UnmodifiableMapMixin",
                    "org.springframework.security.oauth2.server.authorization.jackson2.OAuth2TokenFormatMixin",
                    "java.time.Duration",
                    "org.springframework.security.core.GrantedAuthority",
                    "org.springframework.security.oauth2.jose.jws.SignatureAlgorithm",
                    "java.util.Collections$UnmodifiableMap",
                    "org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat",
                    "org.springframework.security.oauth2.server.authorization.jackson2.UnmodifiableMapDeserializer")) {
                var typeReference = TypeReference.of(type);
                var values = MemberCategory.values();
                hints.reflection().registerType(typeReference, values);
                try {
                    var clzz = Class.forName(typeReference.getName());
                    if (Serializable.class.isAssignableFrom(clzz)) {
                        hints.serialization().registerType(typeReference);
                        System.out.println("registering serialization hint for " + typeReference.getName() + '.');
                    }
                } //
                catch (Throwable t) {
                    System.out.println("couldn't register serialization hint for " + typeReference.getName() + ":" + t.getMessage());
                }

            }

            for (var folder : Set.of("data", "schema"))
                hints.resources().registerPattern("sql/" + folder + "/*sql");

            registerSecurityJacksonModules(hints);

        }

        private static void registerSecurityJacksonModules(RuntimeHints hints) {


            Set.of(

                            "org.springframework.security.jackson2.CoreJackson2Module",
                            "org.springframework.security.cas.jackson2.CasJackson2Module",
                            "org.springframework.security.web.jackson2.WebJackson2Module",
                            "org.springframework.security.web.server.jackson2.WebServerJackson2Module",
                            "org.springframework.security.web.jackson2.WebServletJackson2Module",
                            "org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module",
                            "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
                            "org.springframework.security.ldap.jackson2.LdapJackson2Module",
                            "org.springframework.security.saml2.jackson2.Saml2Jackson2Module")
                    .forEach(cn -> hints.reflection().registerType(TypeReference.of(cn), MemberCategory.values()));


            var set = new HashSet<Class<?>>();
            ClassLoader classLoader = AotConfiguration.class.getClassLoader();
            List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
            var om = new ObjectMapper();
            var sc = new AccumulatingSetupContext(om, set);

            for (var module : securityModules) {
                set.add(module.getClass());
                module.setupModule(sc);
                module.getDependencies().forEach(m -> set.add(m.getClass()));
            }

            set.forEach(c -> {
                System.out.println("registering security related type " + c.getName() + '.');
                hints.reflection().registerType(c, MemberCategory.values());
            });
        }
    }

    static class AccumulatingSetupContext implements Module.SetupContext {


        final Collection<Class<?>> classesToRegister;

        final ObjectMapper objectMapper;

        AccumulatingSetupContext(ObjectMapper objectMapper, Collection<Class<?>> classes) {
            this.objectMapper = objectMapper;
            this.classesToRegister = classes;
        }

        @Override
        public Version getMapperVersion() {
            return null;
        }

        @Override
        public <C extends ObjectCodec> C getOwner() {
            return (C) this.objectMapper;
        }

        @Override
        public TypeFactory getTypeFactory() {
            return null;
        }

        @Override
        public boolean isEnabled(MapperFeature f) {
            return false;
        }

        @Override
        public boolean isEnabled(DeserializationFeature f) {
            return false;
        }

        @Override
        public boolean isEnabled(SerializationFeature f) {
            return false;
        }

        @Override
        public boolean isEnabled(JsonFactory.Feature f) {
            return false;
        }

        @Override
        public boolean isEnabled(JsonParser.Feature f) {
            return false;
        }

        @Override
        public boolean isEnabled(JsonGenerator.Feature f) {
            return false;
        }

        @Override
        public MutableConfigOverride configOverride(Class<?> type) {
            this.classesToRegister.add(type);
            return null;
        }

        @Override
        public void addDeserializers(Deserializers d) {

        }

        @Override
        public void addKeyDeserializers(KeyDeserializers s) {

        }

        @Override
        public void addSerializers(Serializers s) {

        }

        @Override
        public void addKeySerializers(Serializers s) {

        }

        @Override
        public void addBeanDeserializerModifier(BeanDeserializerModifier mod) {

        }

        @Override
        public void addBeanSerializerModifier(BeanSerializerModifier mod) {

        }

        @Override
        public void addAbstractTypeResolver(AbstractTypeResolver resolver) {

        }

        @Override
        public void addTypeModifier(TypeModifier modifier) {

        }

        @Override
        public void addValueInstantiators(ValueInstantiators instantiators) {

        }

        @Override
        public void setClassIntrospector(ClassIntrospector ci) {

        }

        @Override
        public void insertAnnotationIntrospector(AnnotationIntrospector ai) {

        }

        @Override
        public void appendAnnotationIntrospector(AnnotationIntrospector ai) {

        }

        @Override
        public void registerSubtypes(Class<?>... subtypes) {
            this.classesToRegister.addAll(Stream.of(subtypes).collect(Collectors.toSet()));
        }

        @Override
        public void registerSubtypes(NamedType... subtypes) {
            this.classesToRegister.addAll(Stream.of(subtypes).map(NamedType::getType).collect(Collectors.toSet()));
        }

        @Override
        public void registerSubtypes(Collection<Class<?>> subtypes) {
            this.classesToRegister.addAll(subtypes);
        }

        @Override
        public void setMixInAnnotations(Class<?> target, Class<?> mixinSource) {
            this.classesToRegister.add(target);
            this.classesToRegister.add(mixinSource);
        }

        @Override
        public void addDeserializationProblemHandler(DeserializationProblemHandler handler) {

        }

        @Override
        public void setNamingStrategy(PropertyNamingStrategy naming) {

        }
    }


}

