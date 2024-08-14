/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.metric;

/**
 * RouteConfigurationEnum
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 8/12/2024
 * @since 1.0.0
 */
public enum RouteConfigurationEnum {
	NAME("Name", "name"),
	TYPE("Type", "mode"),
	PROTOCOL("Protocol", "protocol"),
	ADDRESS("Address", "address"),
	STATUS("Status", "summaryStatusDetails"),
	;
	private final String name;
	private final String field;

	/**
	 * Constructor for RouteInfoMetric.
	 *
	 * @param name The name representing the system information category.
	 * @param field The field associated with the category.
	 */
	RouteConfigurationEnum(String name, String field) {
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
