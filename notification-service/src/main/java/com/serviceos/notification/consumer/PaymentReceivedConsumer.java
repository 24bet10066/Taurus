package com.serviceos.notification.consumer;

import com.serviceos.notification.channel.NotificationDispatcher;
import com.serviceos.shared.event.PaymentReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Sends the customer a WhatsApp receipt as soon as payment is recorded.
 *
 * This is the single warmest signal the shop sends — the customer knows the
 * shop got their money, sees the amount and a reference, and feels closed-loop.
 * Reduces "did you receive my payment?" callbacks.
 *
 * The cooldown on PAYMENT_RECEIPT is intentionally bypassed (emergency=true);
 * receipts must not be deduplicated even if the customer pays twice in one day.
 */
@Component
public class PaymentReceivedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentReceivedConsumer.class);
    private static final String MESSAGE_TYPE = "PAYMENT_RECEIPT";

    private final NotificationDispatcher dispatcher;

    public PaymentReceivedConsumer(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = PaymentReceivedEvent.TOPIC, groupId = "notification-service")
    public void onPaymentReceived(PaymentReceivedEvent event) {
        if (event.amount() == null || event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Skipping payment.received with zero/null amount paymentId={}", event.paymentId());
            return;
        }
        if (event.customerPhone() == null || event.customerPhone().isBlank()) {
            log.warn("payment.received missing customerPhone — cannot send receipt paymentId={}",
                    event.paymentId());
            return;
        }

        String amount = event.amount().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
        String shortJobId = shortJobId(event.jobId());
        String method = event.method() != null
                ? methodLabel(event.method().name())
                : "received";

        String body = "SK Electronics — Payment received ✅\n"
                + "₹" + amount + " by " + method + "\n"
                + "Job #" + shortJobId + "\n"
                + "Thank you! Shukriya 🙏";

        var result = dispatcher.dispatch(event.customerPhone(), body, MESSAGE_TYPE, true);
        log.info("Payment receipt: paymentId={} amount=₹{} channel={} success={} err={}",
                event.paymentId(), amount, result.channel(), result.success(), result.error());
    }

    private static String shortJobId(java.util.UUID id) {
        if (id == null) return "—";
        String s = id.toString().replace("-", "");
        return s.substring(s.length() - 6).toUpperCase();
    }

    private static String methodLabel(String m) {
        return switch (m) {
            case "CASH"     -> "cash";
            case "RAZORPAY" -> "online";
            case "UPI"      -> "UPI";
            default         -> m.toLowerCase();
        };
    }
}
