package com.serviceos.notification.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock WhatsAppChannel whatsApp;
    @Mock SmsChannel sms;

    NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(whatsApp, sms);
    }

    @Test
    void dispatch_whatsappSucceeds_returnWhatsappResult() {
        when(whatsApp.send("9876543210", "Hello")).thenReturn(true);

        SendResult result = dispatcher.dispatch("9876543210", "Hello");

        assertThat(result.success()).isTrue();
        assertThat(result.channel()).isEqualTo("whatsapp");
        verify(sms, never()).send(any(), any());
    }

    @Test
    void dispatch_whatsappFails_fallsBackToSms() {
        when(whatsApp.send("9876543210", "Hello")).thenReturn(false);
        when(sms.send("9876543210", "Hello")).thenReturn(true);

        SendResult result = dispatcher.dispatch("9876543210", "Hello");

        assertThat(result.success()).isTrue();
        assertThat(result.channel()).isEqualTo("sms");
    }

    @Test
    void dispatch_bothFail_returnFailure() {
        when(whatsApp.send(any(), any())).thenReturn(false);
        when(sms.send(any(), any())).thenReturn(false);

        SendResult result = dispatcher.dispatch("9876543210", "Hello");

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo("all channels failed");
    }

    @Test
    void dispatchBoth_sendsOnBothChannels() {
        when(whatsApp.send(any(), any())).thenReturn(true);
        when(sms.send(any(), any())).thenReturn(true);

        dispatcher.dispatchBoth("9876543210", "Cancelled");

        verify(whatsApp).send("9876543210", "Cancelled");
        verify(sms).send("9876543210", "Cancelled");
    }

    @Test
    void dispatchBoth_whatsappFails_stillSendsSms() {
        when(whatsApp.send(any(), any())).thenReturn(false);
        when(sms.send(any(), any())).thenReturn(true);

        dispatcher.dispatchBoth("9876543210", "Cancelled");

        verify(sms).send("9876543210", "Cancelled");
    }

    @Test
    void sendResult_okFactory_setsFields() {
        SendResult r = SendResult.ok("whatsapp");
        assertThat(r.success()).isTrue();
        assertThat(r.channel()).isEqualTo("whatsapp");
        assertThat(r.error()).isNull();
    }

    @Test
    void sendResult_failFactory_setsFields() {
        SendResult r = SendResult.fail("timeout");
        assertThat(r.success()).isFalse();
        assertThat(r.channel()).isNull();
        assertThat(r.error()).isEqualTo("timeout");
    }
}
