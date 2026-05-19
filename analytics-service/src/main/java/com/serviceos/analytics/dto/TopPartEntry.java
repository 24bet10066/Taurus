package com.serviceos.analytics.dto;

import java.util.UUID;

public record TopPartEntry(UUID partId, String partName, int soldCount) {}
