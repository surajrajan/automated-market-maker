package com.client.kms;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.client.kms.token.SwapClaimToken;
import com.config.ErrorMessages;
import com.model.exception.InvalidInputException;
import com.util.ObjectMapperUtil;
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
        final CryptoResult<byte[], KmsMasterKey> encryptResult = awsCrypto.encryptData(this.keyProvider,
                input.getBytes(StandardCharsets.UTF_8));
        final byte[] ciphertext = encryptResult.getResult();
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    public SwapClaimToken decrypt(final String inputText) throws InvalidInputException {
        final SwapClaimToken swapClaimToken;
        try {
            final byte[] cipherText = Base64.getDecoder().decode(inputText);
            final CryptoResult<byte[], KmsMasterKey> decryptResult = awsCrypto.decryptData(this.keyProvider, cipherText);
            String decryptedSwapClaimToken = new String(decryptResult.getResult(), StandardCharsets.UTF_8);
            return ObjectMapperUtil.toClass(decryptedSwapClaimToken, SwapClaimToken.class);
        } catch (Exception e) {
            log.error("Error decrypting payload.", e);
            throw new InvalidInputException(ErrorMessages.INVALID_CLAIM);
        }
    }
}
