/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public enum PingMode {
	ICMP("ICMP"), TCP("TCP");
	private static final Log logger = LogFactory.getLog(PingMode.class);

	private String mode;

	PingMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Retrieve {@link PingMode} instance based on the text value of the mode
	 *
	 * @param mode name of the mode to retrieve
	 * @return instance of {@link PingMode}
	 */
	public static PingMode ofString(String mode) {
		if (logger.isDebugEnabled()) {
			logger.debug("Requested PING mode: " + mode);
		}
		return Arrays.stream(values())
				.filter(pingMode -> Objects.equals(mode, pingMode.mode))
				.findFirst()
				.orElse(ICMP);
	}
}
