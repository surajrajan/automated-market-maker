package com.client.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.SwapContract;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Setter
@Slf4j
@Singleton
public class SQSClient {

    AmazonSQS amazonSQS;
    final private String queueUrl;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public SQSClient(final AmazonSQS amazonSQS) {
        this.amazonSQS = amazonSQS;
        this.queueUrl = amazonSQS.getQueueUrl(SQSConstants.SWAP_REQUESTS_QUEUE_NAME).getQueueUrl();
        log.info("queueUrl: {}", this.queueUrl);
    }

    /**
     * Submits a SwapRequest to the SQS queue.
     *
     * @param swapRequest
     */
    public void submitSwapContract(final SwapContract swapRequest) {
        final String messageBody;
        try {
            messageBody = objectMapper.writeValueAsString(swapRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize swapRequest", e);
        }
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setQueueUrl(queueUrl);
        sendMessageRequest.setMessageBody(messageBody);
        amazonSQS.sendMessage(sendMessageRequest);
    }
}
