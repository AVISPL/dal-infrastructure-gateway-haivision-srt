/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common;

/**
 * Enum representing various the command.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 8/1/2024
 * @since 1.0.0
 */
public class HaivisionCommand {
	public final static String API_SESSION ="api/session";
	public final static String GET_DEVICE_INFO ="api/devices";
	public final static String GET_ALL_ROUTE ="api/gateway/%s/routes?page=1&pageSize=500";
}
