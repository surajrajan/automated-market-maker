package com.client.kms;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import dagger.Module;
import dagger.Provides;
import lombok.extern.slf4j.Slf4j;

import static com.client.kms.KMSConstants.KEY_ARN;

@Module
@Slf4j
public class KMSModule {

    @Provides
    public AWSKMS awsKMS() {
        // 1. Instantiate the SDK
        // This builds the AwsCrypto client with the RequireEncryptRequireDecrypt commitment policy,
        // which enforces that this client only encrypts using committing algorithm suites and enforces
        // that this client will only decrypt encrypted messages that were created with a committing algorithm suite.
        // This is the default commitment policy if you build the client with `AwsCrypto.builder().build()`
        // or `AwsCrypto.standard()`.
        return AWSKMSClientBuilder.defaultClient();
    }

    @Provides
    public AwsCrypto awsCrypto() {
        return AwsCrypto.builder()
                .withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
                .build();
    }

    @Provides
    public KmsMasterKeyProvider kmsMasterKeyProvider() {
        return KmsMasterKeyProvider.builder().buildStrict(KEY_ARN);
    }


    @Provides
    public KMSClient kmsClient() {
        return new KMSClient(awsCrypto(), kmsMasterKeyProvider());
    }
}
