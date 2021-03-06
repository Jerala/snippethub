package edu.snippethub.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    private static final String GRANT_TYPE_PASSWORD = "password";

    private static final String REFRESH_TOKEN = "refresh_token";

    private static final String SCOPE_READ = "read";

    private static final String SCOPE_WRITE = "write";

    private static final String TRUST = "trust";

    private static final int ACCESS_TOKEN_VALIDITY_SECONDS = (int) TimeUnit.HOURS.toSeconds(1);

    private static final int REFRESH_TOKEN_VALIDITY_SECONDS = (int) TimeUnit.HOURS.toSeconds(6);

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;

    private final TokenStore tokenStore;

    private final AccessTokenConverter accessTokenConverter;

    @Resource(name = "userService")
    private UserDetailsService userDetailsService;

    @Autowired
    public AuthorizationServerConfig(final AuthenticationManager authenticationManager, final TokenStore tokenStore,
                                     final AccessTokenConverter accessTokenConverter, final PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.accessTokenConverter = accessTokenConverter;
    }

    @Override
    public void configure(final ClientDetailsServiceConfigurer configurer) throws Exception {
        //F-
        configurer.inMemory()
                .withClient("client-id")
                .secret(passwordEncoder.encode("secret"))
                .authorizedGrantTypes(GRANT_TYPE_PASSWORD, REFRESH_TOKEN)
                .scopes(SCOPE_READ, SCOPE_WRITE, TRUST)
                .accessTokenValiditySeconds(ACCESS_TOKEN_VALIDITY_SECONDS)
                .refreshTokenValiditySeconds(REFRESH_TOKEN_VALIDITY_SECONDS);
        //F+
    }

    @Override
    public void configure(final AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.tokenStore(tokenStore)
                .userDetailsService(userDetailsService)
                .authenticationManager(authenticationManager)
                .accessTokenConverter(accessTokenConverter)
                .exceptionTranslator(e -> {
                    // change error code to 401 if bad credentials
                    if(e instanceof InvalidGrantException) {
                        InvalidGrantException invalidGrantException = (InvalidGrantException) e;
                        int errorCode = 401;
                        return ResponseEntity
                                .status(errorCode)
                                .body(new InvalidGrantException(invalidGrantException.getMessage()));
                    }
                    else if (e instanceof OAuth2Exception) {
                        OAuth2Exception oAuth2Exception = (OAuth2Exception) e;

                        return ResponseEntity
                                .status(oAuth2Exception.getHttpErrorCode())
                                .body(OAuth2Exception.create(oAuth2Exception.getOAuth2ErrorCode(),
                                        oAuth2Exception.getMessage()));
                    }
                    else {
                        throw e;
                    }
                });
    }
}