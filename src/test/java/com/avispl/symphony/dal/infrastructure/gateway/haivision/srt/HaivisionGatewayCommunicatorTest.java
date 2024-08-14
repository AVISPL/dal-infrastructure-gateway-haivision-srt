/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt;


import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;

public class HaivisionGatewayCommunicatorTest {
	private ExtendedStatistics extendedStatistics;
	private HaivisionGatewayCommunicator haivisionGatewayCommunicator;
	private ExtendedStatistics extendedStatistic;

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
		haivisionGatewayCommunicator.setFilterByRouteName("000-Avid-Loopback");
		extendedStatistic = (ExtendedStatistics) haivisionGatewayCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals(11, statistics.size());
	}
}
