/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt;


import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
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

	/**
	 * Test device info
	 *
	 */
	@Test
	void testGetDeviceInfo() throws Exception {
		haivisionGatewayCommunicator.setFilterAllRouteName("");
		haivisionGatewayCommunicator.setFilterAllRouteName("");
		haivisionGatewayCommunicator.setFilterByRouteName("000-Avid-Loopback");
		extendedStatistic = (ExtendedStatistics) haivisionGatewayCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals(12, statistics.size());
		Assert.assertEquals("Cp0TY9ajND9wVhDmzJT0ww", statistics.get("DeviceID"));
		Assert.assertEquals("Haivision Media Gateway", statistics.get("DeviceName"));
		Assert.assertEquals("5.5.230907.1727", statistics.get("FirmwareVersion"));
		Assert.assertEquals("127.0.0.1", statistics.get("IPAddress"));
		Assert.assertEquals("Aug 14, 2024, 9:00 AM", statistics.get("LastConnected"));
		Assert.assertEquals("<1m", statistics.get("LastConnection"));
		Assert.assertEquals("False", statistics.get("PendingSync"));
		Assert.assertEquals("VMware-42358b0285d8a8d8-8351cd837767f417", statistics.get("SerialNumber"));
		Assert.assertEquals("Online", statistics.get("Status"));
		Assert.assertEquals("Ok", statistics.get("StatusCode"));
		Assert.assertEquals("Connection has been established in the last 1 minutes.", statistics.get("StatusDetails"));
		Assert.assertEquals("Gateway", statistics.get("Type"));
	}

	@Test
	void testDeviceInfoWithFilteringAllRouteName() throws Exception{
		haivisionGatewayCommunicator.setFilterAllRouteName("true");
		extendedStatistic = (ExtendedStatistics) haivisionGatewayCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals(661, statistics.size());
	}

	@Test
	void testDeviceInfoWithoutFilteringAllRouteName() throws Exception{
		haivisionGatewayCommunicator.setFilterAllRouteName("false");
		extendedStatistic = (ExtendedStatistics) haivisionGatewayCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals(13, statistics.size());
	}

	/**
	 * Tests the retrieval of aggregated data with a specific device type filter applied, verifying the statistics accordingly.
	 *
	 * @throws Exception if there's an error during the test process.
	 */
	@Test
	void testDeviceInfoWithFiltering() throws Exception {
		haivisionGatewayCommunicator.setFilterByRouteName("0-DK-PlaySTB");
		extendedStatistic = (ExtendedStatistics) haivisionGatewayCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals("SRT", statistics.get("0-DK-PlaySTB#SourceProtocol"));
		Assert.assertEquals("SRT", statistics.get("0-DK-PlaySTB#DestinationProtocol"));

	}

	/**
	 * Tests the retrieval of aggregated data with multiple device type filters applied, verifying the resulting statistics.
	 *
	 * @throws Exception if there's an error during the test process.
	 */
	@Test
	void testAggregatorWithMultipleFilteringValue() throws Exception {
		haivisionGatewayCommunicator.setFilterByRouteName("0-DK-PlaySTB, 4Kp60 Live (HMP) b272e17b-26f6-47e4-b25b-f8297121c8e0, RY - Play, STD, 000-Avid-Loopback, BT");;
		extendedStatistic = (ExtendedStatistics) haivisionGatewayCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		System.out.println(statistics);
	}
}
