package com.serviceos.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.serviceos.shared.exception.BusinessRuleViolationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayService(@Value("${serviceos.razorpay.key-id:}") String keyId,
                           @Value("${serviceos.razorpay.key-secret:}") String keySecret,
                           @Value("${serviceos.razorpay.webhook-secret:}") String webhookSecret) {
        this.keyId         = keyId;
        this.keySecret     = keySecret;
        this.webhookSecret = webhookSecret;
    }

    public record RazorpayOrderResult(String orderId, BigDecimal amount, String currency) {}

    public RazorpayOrderResult createOrder(UUID jobId, BigDecimal amount) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            // Razorpay requires amount in smallest currency unit (paise)
            options.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            options.put("currency", "INR");
            options.put("receipt", "job_" + jobId);
            options.put("notes", new JSONObject().put("jobId", jobId.toString()));
            Order order = client.orders.create(options);
            String orderId = order.get("id");
            log.debug("Razorpay order created: {} for jobId={}", orderId, jobId);
            return new RazorpayOrderResult(orderId, amount, "INR");
        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed for jobId={}: {}", jobId, ex.getMessage());
            throw new BusinessRuleViolationException("RAZORPAY_ERROR",
                    "Payment gateway error: " + ex.getMessage());
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception ex) {
            log.error("Signature verification error: {}", ex.getMessage());
            return false;
        }
    }

    /** Verifies X-Razorpay-Signature on incoming webhook payloads using the webhook secret. */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("RAZORPAY_WEBHOOK_SECRET not configured — rejecting webhook");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception ex) {
            log.error("Webhook signature verification error: {}", ex.getMessage());
            return false;
        }
    }
}
