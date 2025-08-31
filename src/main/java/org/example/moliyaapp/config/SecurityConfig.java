package org.example.moliyaapp.config;

import org.example.moliyaapp.filter.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Autowired
    public SecurityConfig(
            JwtFilter jwtFilter,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint/*, CustomAuthenticationSuccessHandler handler*/) {
        this.jwtFilter = jwtFilter;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) //changed from disabled to defaults
                .authorizeHttpRequests(authRequest ->
                        authRequest
                                .requestMatchers(WHITE_LIST).permitAll()
//                                .requestMatchers(BLACK_LIST).permitAll()
                                .requestMatchers(HttpMethod.POST, "/auths/login").permitAll()
                                .requestMatchers("/fee-images/**").permitAll()  // allow images
                                .requestMatchers(HttpMethod.POST, "/auths/register-employee").permitAll()
//                                .requestMatchers("/auths/**").permitAll()
                                .anyRequest()
                                .authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .build();
    }

    private static final String[] WHITE_LIST = {

            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/swagger-resources/**",//added
            "/v3/api-docs/**",
            "/v2/api-docs", //added
            "/actuator/**"

    };
    private static final String[] BLACK_LIST = {
            "/api/v1/employees/**",
            "/api/v1/positions/**",
            "/api/v1/groups/**",
            "/api/v1/employees/**",
            "/monthlyFee/**",
            "/expense/**",
//            "/api/v1/companies/**",
            "/api/v1/subjects/**",
            "/auths/**",
            "/role/**",
            "/api/v1/roles/**",
            "/users/**",

            "/category/**",
            "/teacherTable/**",
            "/teacherContracts/**",
            "/extra-lesson-price/**",
            "/api/v1/transactions/**",
            "/studentTariffs/**",
            "/api/v1/sms/send/**",
            "/api/reminder/**",
            "notify.eskiz.uz/api/auth/**",
            "/notify.eskiz.uz/api/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://192.168.1.183",
                "http://192.168.50.173:3000",
                "http://192.168.50.173",
                "http://10.173.41.173",
                "http://192.168.56.1",
                "http://192.168.10.73",
                "http://192.168.10.241",
                "http://192.168.10.1",
                "http://10.61.189.173:3000",
                "http://10.61.189.173",
                "http://192.168.0.117:3000",
                "http://192.168.0.117",
                "http://192.168.10.241:3000",
                "http://192.168.141.173:3000",
                "http://192.168.115.173:3000",
                "http://192.168.211.173:3000",
                "http://192.168.211.173",
                "http://192.168.115.173",
                "http://192.168.141.173",
                "https://istiqlol.iftixormaktabi.uz"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization")); // Optional
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source; // ✅ Correct bean type
    }
//
//    @Bean
//    public CorsFilter corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        configuration.addAllowedOriginPattern("*"); //disabled
//        configuration.setAllowedHeaders(List.of("*"));
//        configuration.setExposedHeaders(List.of("*"));
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
//        configuration.setAllowCredentials(true);
//        configuration.setAllowedOrigins(List.of(
//                "http://localhost:3000",
//                "http://192.168.1.183/",
//                "http://192.168.81.173/",
//                "https://iftixor-moliya-crm.vercel.app/",
//                "https://iftixor-school.netlify.app/",
//                "https://crm.iftixormaktabi.uz"));
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return new CorsFilter(source);
//    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }


}

