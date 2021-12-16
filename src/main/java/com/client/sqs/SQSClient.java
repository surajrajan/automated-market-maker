package com.client.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Setter
@Slf4j
@Singleton
public class SQSClient {

    AmazonSQS amazonSQS;
    final private String queueUrl;

    public SQSClient(final AmazonSQS amazonSQS) {
        this.amazonSQS = amazonSQS;
        this.queueUrl = amazonSQS.getQueueUrl(SQSConstants.SWAP_REQUESTS_QUEUE_NAME).getQueueUrl();
        log.info("queueUrl: {}", this.queueUrl);
    }

    /**
     * Submits a SwapRequest to the SQS queue.
     *
     * @param messageBody
     */
    public void submitMessage(final String messageBody) {
        log.info("Submitting message to SQS: {}", messageBody);
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setQueueUrl(queueUrl);
        sendMessageRequest.setMessageBody(messageBody);
        amazonSQS.sendMessage(sendMessageRequest);
    }
}
