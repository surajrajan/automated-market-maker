package com.client.sqs;

import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import dagger.Module;
import dagger.Provides;

@Module
public class SQSModule {

    @Provides
    public SQSClient sqsClient() {
        return new SQSClient(AmazonSQSClientBuilder.defaultClient());
    }
}
