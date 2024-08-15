/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.metric;

/**
 * Enum representing various route info metrics.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 8/12/2024
 * @since 1.0.0
 */
public enum RouteInfoEnum {
	UPTIME("RouteUptime", "elapsedTime"),
	ID("RouteID", "id"),
	STATUS("RouteStatus", "summaryStatusDetails"),
	SOURCE("Source", "source"),
	DESTINATION("Destinations", "destinations"),
			;
	private final String name;
	private final String field;

	/**
	 * Constructor for RouteInfoMetric.
	 *
	 * @param name The name representing the system information category.
	 * @param field The field associated with the category.
	 */
	RouteInfoEnum(String name, String field) {
		this.name = name;
		this.field = field;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #field}
	 *
	 * @return value of {@link #field}
	 */
	public String getField() {
		return field;
	}
}
