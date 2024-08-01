/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;

public class HaivisionGatewayCommunicatorTest {
	private ExtendedStatistics extendedStatistics;
	private HaivisionGatewayCommunicator haivisionGatewayCommunicator;

	@BeforeEach
	void setUp() throws Exception {
		haivisionGatewayCommunicator = new HaivisionGatewayCommunicator();
		haivisionGatewayCommunicator.setTrustAllCertificates(true);
		haivisionGatewayCommunicator.setHost("");
		haivisionGatewayCommunicator.setLogin("");
		haivisionGatewayCommunicator.setPassword("");
		haivisionGatewayCommunicator.setPort(443);
		haivisionGatewayCommunicator.init();
		haivisionGatewayCommunicator.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		haivisionGatewayCommunicator.disconnect();
		haivisionGatewayCommunicator.destroy();
	}

	@Test
	void testLoginSuccess() throws Exception {
		haivisionGatewayCommunicator.getMultipleStatistics();
	}
}
