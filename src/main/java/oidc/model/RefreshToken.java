package oidc.model;

import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshToken extends AccessToken {

    private String accessTokenValue;

    public RefreshToken(String value, String sub, String clientId, List<String> scopes, Date expiresIn, String accessTokenValue) {
        super(value, sub, clientId, scopes, expiresIn);
        this.accessTokenValue = accessTokenValue;
    }
}