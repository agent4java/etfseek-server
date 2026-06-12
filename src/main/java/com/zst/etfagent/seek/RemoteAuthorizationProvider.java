package com.zst.etfagent.seek;

import com.zst.etfagent.config.EtfAgentProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RemoteAuthorizationProvider {

    private final EtfAgentProperties properties;

    public RemoteAuthorizationProvider(EtfAgentProperties properties) {
        this.properties = properties;
    }

    public Optional<String> currentAuthorization() {
        String authorization = properties.getAuthorization();
        if (authorization == null || authorization.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(authorization.trim());
    }
}
