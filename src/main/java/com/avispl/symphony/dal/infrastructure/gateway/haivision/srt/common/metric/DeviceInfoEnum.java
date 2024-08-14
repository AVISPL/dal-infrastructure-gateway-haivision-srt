/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.metric;

public enum DeviceInfoEnum {
	DEVICE_ID("DeviceID", "_id"),
	TYPE("Type", "type"),
	IP_ADDRESS("IPAddress", "ip"),
	DEVICE_NAME("DeviceName", "name"),
	LAST_CONNECTED("LastConnected", "lastConnectedAt"),
	STATUS_CODE("StatusCode", "statusCode"),
	STATUS("Status", "status"),
	STATUS_DETAILS("StatusDetails", "statusDetails"),
	SERIAL_NUMBER("SerialNumber", "serialNumber"),
	FIRMWARE_VERSION("FirmwareVersion", "firmware"),
	HAS_ADMIN_ERROR("HasAdminError", "hasAdminError"),
	PENDING_SYNC("PendingSync", "pendingSync"),
	LAST_CONNECTION("LastConnection", "lastConnection"),
	;
	private final String name;
	private final String field;

	/**
	 * Constructor for DeviceInfoMetric.
	 *
	 * @param name The name representing the system information category.
	 * @param field The field associated with the category.
	 */
	DeviceInfoEnum(String name, String field) {
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
