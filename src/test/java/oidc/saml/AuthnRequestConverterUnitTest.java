package oidc.saml;

import com.nimbusds.jwt.SignedJWT;
import oidc.model.OpenIDClient;
import oidc.model.Scope;
import oidc.repository.AuthenticationRequestRepository;
import oidc.repository.OpenIDClientRepository;
import oidc.secure.SignedJWTTest;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.RequestCache;

import javax.servlet.http.HttpServletRequest;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthnRequestConverterUnitTest extends AbstractSamlUnitTest implements SignedJWTTest {

    private OpenIDClientRepository openIDClientRepository = mock(OpenIDClientRepository.class);
    private AuthenticationRequestRepository authenticationRequestRepository = mock(AuthenticationRequestRepository.class);
    private RequestCache requestCache = mock(RequestCache.class);

    private AuthnRequestConverter subject ;

    @Before
    public void beforeTest() throws Exception {
        subject = new AuthnRequestConverter(openIDClientRepository, authenticationRequestRepository, requestCache);
        OpenIDClient openIDClient = new OpenIDClient("clientId", singletonList("http://redirect"), singletonList(new Scope("openid")), singletonList("authorization_code"));
        String cert = readFile("keys/certificate.crt");
        setCertificateFields(openIDClient, cert, null, null);
        when(openIDClientRepository.findByClientId("mock_sp")).thenReturn(openIDClient);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "http://localhost/oidc/authorize");
        request.addParameter("client_id", "mock_sp");
        request.addParameter("response_type", "code");
        request.addParameter("acr_values", "http://loa1");
        request.addParameter("prompt", "login");
        request.addParameter("login_hint", "http://idp");

        String keyID = getCertificateKeyIDFromCertificate(cert);
        SignedJWT signedJWT = signedJWT(openIDClient.getClientId(), keyID, openIDClient.getRedirectUrls().get(0));
        request.addParameter("request", signedJWT.serialize());

        when(requestCache.getRequest(any(HttpServletRequest.class), any()))
                .thenReturn(new DefaultSavedRequest(request, portResolver));
    }

    @Test
    public void testSaml() {
        HttpServletRequest request = new MockHttpServletRequest();
        CustomSaml2AuthenticationRequestContext ctx = new CustomSaml2AuthenticationRequestContext(relyingParty, request);
        AuthnRequest authnRequest = subject.convert(ctx);

        assertTrue(authnRequest.isForceAuthn());
        assertEquals("loa1", authnRequest.getRequestedAuthnContext().getAuthnContextClassRefs().get(0).getAuthnContextClassRef());
    }

}