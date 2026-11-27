package com.solidariaplus.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("solidaria.security")
public class SolidariaSecurityProperties {

    private String issuerUri;
    private String jwkSetUri;
    private String audience = "solidaria-api";
    private String revokedSessionsUri;

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getRevokedSessionsUri() {
        return revokedSessionsUri;
    }

    public void setRevokedSessionsUri(String revokedSessionsUri) {
        this.revokedSessionsUri = revokedSessionsUri;
    }
}
