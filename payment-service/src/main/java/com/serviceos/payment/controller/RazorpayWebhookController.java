package com.serviceos.payment.controller;

import com.serviceos.payment.service.PaymentService;
import com.serviceos.payment.service.RazorpayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhook")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayService razorpayService;
    private final PaymentService paymentService;

    public RazorpayWebhookController(RazorpayService razorpayService,
                                     PaymentService paymentService) {
        this.razorpayService = razorpayService;
        this.paymentService  = paymentService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (signature == null || !razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Razorpay webhook rejected — invalid or missing signature");
            return ResponseEntity.badRequest().build();
        }

        paymentService.handleRazorpayWebhook(rawBody);
        return ResponseEntity.ok().build();
    }
}
