package com.client.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import dagger.Module;
import dagger.Provides;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Module
public class JWTModule {

    @Provides
    public Algorithm algorithm() {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize JWTModule.");
        }
        kpg.initialize(1024);
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey rPubKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey rPriKey = (RSAPrivateKey) kp.getPrivate();
        Algorithm algorithmRS = Algorithm.RSA256(rPubKey, rPriKey);
        return algorithmRS;
    }

    @Provides
    public JWTClient jwtClient() {
        return new JWTClient(algorithm());
    }
}