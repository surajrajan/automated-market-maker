package com.client.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.SwapContract;
import lombok.Setter;

import java.util.Date;

@Setter
public class JWTClient {

    private Algorithm algorithm;
    private ObjectMapper objectMapper = new ObjectMapper();

    public JWTClient(final Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getJwtClaim(final SwapContract swapContract, final Date expiresAt) {
        try {
            String swapContractAsString = objectMapper.writeValueAsString(swapContract);
            String token = JWT.create()
                    .withIssuer("AutomatedMarketMaker")
                    .withClaim("swapContract", swapContractAsString)
                    .withExpiresAt(expiresAt)
                    .sign(algorithm);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize / create jwt token", e);
        }
    }


    public SwapContract verifyJwtClaim(final String jwtClaim) {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("pk-signing-example")
                .build();
        DecodedJWT jwt = verifier.verify(jwtClaim);
        SwapContract swapContract = null;
        try {
            swapContract = objectMapper.readValue(jwt.getPayload(), SwapContract.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize jwt token", e);
        }
        return swapContract;
    }
}
