package com.serviceos.job.dto.response;

import java.util.UUID;

public record PublicBookingResponse(
        UUID bookingId,
        String message
) {}
