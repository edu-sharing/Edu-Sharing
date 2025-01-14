package org.edu_sharing.repository.server.tools.security;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Slf4j
public class JwtTokenUtil {

    public static String generateToken(String username, String nodeId, List<String> permissions, String mimeType, String mediaType, String replicationSource) throws GeneralSecurityException {
        Key privateKey = new Signing().getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);

        Date date = new Date();
        String jwt = Jwts.builder()
                .setHeaderParam("kid", ApplicationInfoList.getHomeRepository().getAppId())  // The key id used to sign this
                .setHeaderParam("typ", "JWS") // The type
                .setHeaderParam("repoId", ApplicationInfoList.getHomeRepository().getAppId()) // repoId
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .setIssuer(username)
                .setId(UUID.randomUUID().toString())
                .setSubject("node permissions")
                .claim("repoId", ApplicationInfoList.getHomeRepository().getAppId())
                .claim("node", nodeId)
                .claim("permissions", permissions)
                .claim("mimeType", mimeType)
                .claim("mediaType", mediaType)
                .claim("replicationSource", replicationSource)
                .signWith(privateKey)
                .compact();

        log.info("JWT: {}", jwt);
        return jwt;
    }
}
