package com.client.kms;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@AllArgsConstructor
@Singleton
public class KMSClient {

    private AwsCrypto awsCrypto;
    private KmsMasterKeyProvider keyProvider;

    public String encrypt(final String input) {
        // 4. Encrypt the data
        final CryptoResult<byte[], KmsMasterKey> encryptResult = awsCrypto.encryptData(this.keyProvider, input.getBytes(StandardCharsets.UTF_8));
        final byte[] ciphertext = encryptResult.getResult();
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    public String decrypt(final String inputText) {
        final byte[] cipherText = Base64.getDecoder().decode(inputText);
        // 5. Decrypt the data
        final CryptoResult<byte[], KmsMasterKey> decryptResult = awsCrypto.decryptData(this.keyProvider, cipherText);
        return new String(decryptResult.getResult(), StandardCharsets.UTF_8);
    }
}
