package com.zst.etfagent.seek;

import com.zst.etfagent.config.EtfAgentProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteAuthorizationProviderTest {

    @Test
    void returnsConfiguredAuthorizationToken() {
        EtfAgentProperties properties = new EtfAgentProperties();
        properties.setAuthorization("  Bearer demo-token  ");

        RemoteAuthorizationProvider provider = new RemoteAuthorizationProvider(properties);

        assertThat(provider.currentAuthorization()).contains("Bearer demo-token");
    }

    @Test
    void returnsEmptyWhenTokenIsBlank() {
        EtfAgentProperties properties = new EtfAgentProperties();
        properties.setAuthorization(" ");

        RemoteAuthorizationProvider provider = new RemoteAuthorizationProvider(properties);

        assertThat(provider.currentAuthorization()).isEmpty();
    }
}
