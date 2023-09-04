package bootiful.authorizationserver;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import jakarta.servlet.http.Cookie;
import org.reflections.Reflections;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedCookie;

import java.io.Serializable;
import java.net.URL;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@ImportRuntimeHints(AotConfiguration.Hints.class)
class AotConfiguration {

    static class Hints implements RuntimeHintsRegistrar {


        private Set<Class<?>> subs(Reflections reflections, Class<?>... classesToFind) {
            var all = new HashSet<Class<?>>();
            for (var individualClass : classesToFind) {
                var subTypesOf = reflections.getSubTypesOf(individualClass);
                all.addAll(subTypesOf);
            }
            return all;
        }

        private Set<Class<?>> resolveJacksonTypes() {
            var all = new HashSet<Class<?>>();
            for (var pkg : Set.of("com.fasterxml", "org.springframework")) {
                var reflections = new Reflections(pkg);
                all.addAll(subs(reflections, JsonDeserializer.class, JsonSerializer.class, Module.class));
                all.addAll(reflections.getTypesAnnotatedWith(JsonTypeInfo.class));
                all.addAll(reflections.getTypesAnnotatedWith(JsonAutoDetect.class));
            }
            all.addAll(registerJacksonModuleDeps(all.stream().filter(Module.class::isAssignableFrom).collect(Collectors.toSet())));
            return all;
        }

        private static Collection<Class<?>> registerJacksonModuleDeps(Set<Class<?>> moduleClasses) {
            var set = new HashSet<Class<?>>();
            var classLoader = AotConfiguration.class.getClassLoader();
            var securityModules = new ArrayList<Module>();
            securityModules.addAll(SecurityJackson2Modules.getModules(classLoader));
            securityModules.addAll(moduleClasses
                    .stream()
                    .map(cn -> {
                        try {
                            for (var ctor : cn.getConstructors())
                                if (ctor.getParameterCount() == 0)
                                    return (Module) ctor.newInstance();
                        } //
                        catch (Throwable t) {
                            System.out.println("couldn't construct and inspect module " + cn.getName());
                        }
                        return null;
                    })
                    .collect(Collectors.toSet())
            );
            var om = new ObjectMapper();
            var sc = new AccumulatingSetupContext(om, set);
            for (var module : securityModules) {
                set.add(module.getClass());
                module.setupModule(sc);
                module.getDependencies().forEach(m -> set.add(m.getClass()));
            }

            return set;

        }

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

            var javaClasses = Set.of(ArrayList.class, Date.class, Duration.class, Instant.class, URL.class, TreeMap.class, HashMap.class, LinkedHashMap.class, List.class);

            var savedRequestClasses = Set.of(DefaultSavedRequest.class, SavedCookie.class);

            var oauth2CoreClasses = Set.of(SignatureAlgorithm.class, OAuth2AuthorizationResponseType.class, OAuth2AuthorizationRequest.class, AuthorizationGrantType.class, OAuth2TokenFormat.class, OAuth2Authorization.class, SecurityContextImpl.class);

            var securityClasses = Set.of(User.class, WebAuthenticationDetails.class, GrantedAuthority.class, Principal.class, SimpleGrantedAuthority.class, UsernamePasswordAuthenticationToken.class);

            var servletClasses = Set.of(Cookie.class);

            var jacksonTypes = new HashSet<>(resolveJacksonTypes());
            jacksonTypes.add(SecurityJackson2Modules.class);

            var classes = new ArrayList<Class<?>>();
            classes.addAll(jacksonTypes);
            classes.addAll(servletClasses);
            classes.addAll(oauth2CoreClasses);
            classes.addAll(savedRequestClasses);
            classes.addAll(javaClasses);
            classes.addAll(securityClasses);

            var stringClasses = Map.of(
                    "java.util.", Set.of("Arrays$ArrayList"),
                    "java.util.Collections$", Set.of("UnmodifiableRandomAccessList", "EmptyList", "UnmodifiableMap", "EmptyMap", "SingletonList", "UnmodifiableSet")
            );//

            var all = classes.stream().map(Class::getName).collect(Collectors.toCollection(HashSet::new));
            stringClasses.forEach((root, setOfClasses) -> setOfClasses.forEach(cn -> all.add(root + cn)));

            var memberCategories = MemberCategory.values();

            all.forEach(type -> {
                var typeReference = TypeReference.of(type);
                hints.reflection().registerType(typeReference, memberCategories);
                System.out.println("registering " + type);
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
            });

            Set.of("data", "schema").forEach(folder -> hints.resources().registerPattern("sql/" + folder + "/*sql"));

        }

    }

    static class AccumulatingSetupContext implements Module.SetupContext {

        private final Collection<Class<?>> classesToRegister;

        private final ObjectMapper objectMapper;

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

//@Configuration
class JsonConfiguration {

    @Bean
    ApplicationRunner parse() {
        return a -> {
            var gaList = new com.fasterxml.jackson.core.type.TypeReference<List<GrantedAuthority>>() {
            };
            var objectMapper = new ObjectMapper();
            SecurityJackson2Modules.getModules(ClassLoader.getSystemClassLoader()).forEach(objectMapper::registerModule);
            objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
            var json = """
                        {"@class":"java.util.Collections$UnmodifiableMap","org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest":{"@class":"org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest","authorizationUri":"http://localhost:8080/oauth2/authorize","authorizationGrantType":{"value":"authorization_code"},"responseType":{"value":"code"},"clientId":"crm","redirectUri":"http://127.0.0.1:8082/login/oauth2/code/spring","scopes":["java.util.Collections$UnmodifiableSet",["user.read","openid"]],"state":"QjdbcbnM2uoxnwksbT1IooOOWxNbkdMVV0LDsptQuH4=","additionalParameters":{"@class":"java.util.Collections$UnmodifiableMap","nonce":"ryv3qPgr5IwFA6LYmLf1QkQY4fRtaZmg_ePB2rSJrqQ","continue":""},"authorizationRequestUri":"http://localhost:8080/oauth2/authorize?response_type=code&client_id=crm&scope=user.read%20openid&state=QjdbcbnM2uoxnwksbT1IooOOWxNbkdMVV0LDsptQuH4%3D&redirect_uri=http://127.0.0.1:8082/login/oauth2/code/spring&nonce=ryv3qPgr5IwFA6LYmLf1QkQY4fRtaZmg_ePB2rSJrqQ&continue=","attributes":{"@class":"java.util.Collections$UnmodifiableMap"}},"java.security.Principal":{"@class":"org.springframework.security.authentication.UsernamePasswordAuthenticationToken","authorities":["java.util.Collections$UnmodifiableRandomAccessList",[{"@class":"org.springframework.security.core.authority.SimpleGrantedAuthority","authority":"ROLE_USER"}]],"details":{"@class":"org.springframework.security.web.authentication.WebAuthenticationDetails","remoteAddress":"0:0:0:0:0:0:0:1","sessionId":"745F400BA9E8317369ECFD9B9E826695"},"authenticated":true,"principal":{"@class":"org.springframework.security.core.userdetails.User","password":null,"username":"jlong","authorities":["java.util.Collections$UnmodifiableSet",[{"@class":"org.springframework.security.core.authority.SimpleGrantedAuthority","authority":"ROLE_USER"}]],"accountNonExpired":true,"accountNonLocked":true,"credentialsNonExpired":true,"enabled":true},"credentials":null}}
                    """;
            var jsonNode = objectMapper.readTree(json);
            var authoritiesJsonNode = readJsonNode(jsonNode, "authorities").traverse(objectMapper);
            objectMapper.readValue(authoritiesJsonNode, gaList).forEach(System.out::println);

        };
    }

    private static JsonNode readJsonNode(JsonNode jsonNode, String field) {
        return jsonNode.has(field) ? jsonNode.get(field) : MissingNode.getInstance();
    }
}
