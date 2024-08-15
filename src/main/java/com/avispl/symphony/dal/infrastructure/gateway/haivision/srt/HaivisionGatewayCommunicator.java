/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.haivision.srt;


import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.HaivisionCommand;
import com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.HaivisionConstant;
import com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.PingMode;
import com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.metric.DeviceInfoEnum;
import com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.metric.RouteConfigurationEnum;
import com.avispl.symphony.dal.infrastructure.gateway.haivision.srt.common.metric.RouteInfoEnum;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * HaivisionGatewayCommunicator Adapter
 *
 * Supported features are:
 * Monitoring for Device Information and Route information
 *
 * Monitoring Capabilities:
 * Haivision Media Gateway version 1.0.0
 *
 * DeviceID
 * DeviceName
 * FirmwareVersion
 * IPAddress
 * LastConnected
 * LastConnection
 * PendingSync
 * SerialNumber
 * Status
 * StatusCode
 * StatusDetails
 * Type
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 8/15/2024
 * @since 1.0.0
 */
public class HaivisionGatewayCommunicator extends RestCommunicator implements Monitorable, Controller {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * store authentication information
	 */
	private String authenticationCookie = HaivisionConstant.EMPTY;

	/**
	 * ReentrantLock to prevent telnet session is closed when adapter is retrieving statistics from the device.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Store previous/current ExtendedStatistics
	 */
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * isEmergencyDelivery to check if control flow is trigger
	 */
	private boolean isEmergencyDelivery;

	/**
	 * A cache that maps route names to their corresponding values.
	 */
	private final Map<String, String> cacheValue = new HashMap<>();

	/**
	 * A string that specifies the name of a route to filter by.
	 */
	private String filterByRouteName;

	/**
	 * A string that specifies the names of all routes to filter by.
	 */
	private String filterAllRouteName;

	/**
	 * A set containing all route names.
	 */
	private Set<String> allRouteNameSet = new HashSet<>();

	/**
	 * The ID of the device.
	 */
	private String deviceId;

	/**
	 * Retrieves {@link #filterByRouteName}
	 *
	 * @return value of {@link #filterByRouteName}
	 */
	public String getFilterByRouteName() {
		return filterByRouteName;
	}

	/**
	 * Sets {@link #filterByRouteName} value
	 *
	 * @param filterByRouteName new value of {@link #filterByRouteName}
	 */
	public void setFilterByRouteName(String filterByRouteName) {
		this.filterByRouteName = filterByRouteName;
	}

	/**
	 * Retrieves {@link #filterAllRouteName}
	 *
	 * @return value of {@link #filterAllRouteName}
	 */
	public String getFilterAllRouteName() {
		return filterAllRouteName;
	}

	/**
	 * Sets {@link #filterAllRouteName} value
	 *
	 * @param filterAllRouteName new value of {@link #filterAllRouteName}
	 */
	public void setFilterAllRouteName(String filterAllRouteName) {
		this.filterAllRouteName = filterAllRouteName;
	}

	/**
	 * ping mode
	 */
	private PingMode pingMode = PingMode.ICMP;

	/**
	 * Retrieves {@link #pingMode}
	 *
	 * @return value of {@link #pingMode}
	 */
	public PingMode getPingMode() {
		return pingMode;
	}

	/**
	 * Sets {@link #pingMode} value
	 *
	 * @param pingMode new value of {@link #pingMode}
	 */
	public void setPingMode(PingMode pingMode) {
		this.pingMode = pingMode;
	}

	/**
	 * Constructs a new instance of HaivisionGatewayCommunicator.
	 */
	public HaivisionGatewayCommunicator() throws IOException {
		this.setTrustAllCertificates(true);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 *
	 * Check for available devices before retrieving the value
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (this.pingMode == PingMode.ICMP) {
			return super.ping();
		} else if (this.pingMode == PingMode.TCP) {
			if (isInitialized()) {
				long pingResultTotal = 0L;

				for (int i = 0; i < this.getPingAttempts(); i++) {
					long startTime = System.currentTimeMillis();

					try (Socket puSocketConnection = new Socket(this.host, this.getPort())) {
						puSocketConnection.setSoTimeout(this.getPingTimeout());
						if (puSocketConnection.isConnected()) {
							long pingResult = System.currentTimeMillis() - startTime;
							pingResultTotal += pingResult;
							if (this.logger.isTraceEnabled()) {
								this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
							}
						} else {
							if (this.logger.isDebugEnabled()) {
								this.logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
							}
							return this.getPingTimeout();
						}
					} catch (SocketTimeoutException | ConnectException tex) {
						throw new RuntimeException("Socket connection timed out", tex);
					} catch (UnknownHostException ex) {
						throw new UnknownHostException(String.format("Connection timed out, UNKNOWN host %s", host));
					} catch (Exception e) {
						if (this.logger.isWarnEnabled()) {
							this.logger.warn(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
						}
						return this.getPingTimeout();
					}
				}
				return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
			} else {
				throw new IllegalStateException("Cannot use device class without calling init() first");
			}
		} else {
			throw new IllegalArgumentException("Unknown PING Mode: " + pingMode);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();

		try {
			Map<String, String> stats = new HashMap<>();
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();

			if (!isEmergencyDelivery) {
				checkAuthentication();
				retrieveMonitoringProperties();
				retrieveRouteInfo();
				populateMonitoringProperties(stats);
				populateRouteInfo(stats);
				extendedStatistics.setStatistics(stats);
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		if (CollectionUtils.isEmpty(controllableProperties)) {
			throw new IllegalArgumentException("ControllableProperties can not be null or empty");
		}
		for (ControllableProperty p : controllableProperties) {
			try {
				controlProperty(p);
			} catch (Exception e) {
				logger.error(String.format("Error when control property %s", p.getProperty()), e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() throws Exception {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}

		super.internalInit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (StringUtils.isNotNullOrEmpty(this.authenticationCookie)) {
			deleteCookieSession();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}
		localExtendedStatistics = null;
		cacheValue.clear();
		super.internalDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) throws Exception {
		headers.set("Content-Type", "application/json");
		if (StringUtils.isNotNullOrEmpty(this.authenticationCookie)) {
			headers.set(HaivisionConstant.COOKIE, "sessionID=" + this.authenticationCookie);
		}
		return super.putExtraRequestHeaders(httpMethod, uri, headers);
	}

	/**
	 * Checks and ensures that the authentication cookie is valid.
	 *
	 * If the authentication cookie is missing or invalid, this method will
	 * initiate a new cookie session.
	 *
	 * @throws Exception if an error occurs during the authentication check or session initialization.
	 */
	private void checkAuthentication() throws Exception {
		if (StringUtils.isNullOrEmpty(authenticationCookie)) {
			initialCookieSession();
		} else {
			checkValidCookieSession();
		}
	}

	/**
	 * Initializes a new cookie session by authenticating with the server.
	 *
	 * This method sends the username and password to the server to obtain a new
	 * session ID. If the authentication fails, a {@link FailedLoginException} is thrown.
	 *
	 * @throws FailedLoginException if the login credentials are incorrect or the server rejects the login.
	 * @throws ResourceNotReachableException if the server is not reachable or the authorization token cannot be retrieved.
	 */
	private void initialCookieSession() throws FailedLoginException {
		try {
			Map<String, String> bodyRequest = new HashMap<>();
			bodyRequest.put("username", this.getLogin());
			bodyRequest.put("password", this.getPassword());
			JsonNode response = this.doPost(HaivisionCommand.API_SESSION, bodyRequest, JsonNode.class);
			if (response != null && response.has(HaivisionConstant.RESPONSE) && response.get(HaivisionConstant.RESPONSE).has(HaivisionConstant.SESSION_ID)) {
				this.authenticationCookie = response.get(HaivisionConstant.RESPONSE).get(HaivisionConstant.SESSION_ID).asText();
				return;
			}
			this.authenticationCookie = HaivisionConstant.EMPTY;
			throw new ResourceNotReachableException("Unable to retrieve the authorization token, endpoint not reachable.");
		} catch (FailedLoginException ex) {
			throw new FailedLoginException("Unable to login. Please check device credentials");
		} catch (Exception e) {
			throw new ResourceNotReachableException("Unable to retrieve the authorization token, endpoint not reachable");
		}
	}

	/**
	 * Verifies if the current authentication cookie session is still valid.
	 *
	 * This method sends a request to the server to validate the current session.
	 * If the session is invalid or an error occurs, a new session is initialized.
	 *
	 * @throws Exception if an error occurs during the validation of the cookie session.
	 */
	private void checkValidCookieSession() throws Exception {
		try {
			JsonNode response = this.doGet(HaivisionCommand.API_SESSION, JsonNode.class);
			if (response == null || response.has(HaivisionConstant.ERROR)) {
				initialCookieSession();
			}
		} catch (Exception e) {
			logger.info("Invalid session ID " + this.authenticationCookie);
			initialCookieSession();
		}
	}

	/**
	 * Deletes the current authentication cookie session from the server.
	 *
	 * This method sends a request to the server to delete the current session ID.
	 * After deleting the session, the local authentication cookie is reset to an empty value.
	 */
	private void deleteCookieSession() {
		try {
			this.doDelete(HaivisionCommand.API_SESSION);
			logger.info("Delete session ID " + this.authenticationCookie);
		} catch (Exception e) {
			logger.info("Error white delete session ID " + this.authenticationCookie);
		} finally {
			this.authenticationCookie = HaivisionConstant.EMPTY;
		}
	}

	/**
	 * Retrieves monitoring properties for a device and populates the cache with the device's information.
	 *
	 * @throws ResourceNotReachableException if an error occurs when retrieving the device information.
	 */
	private void retrieveMonitoringProperties() {
		try {
			JsonNode response = this.doGet(HaivisionCommand.GET_DEVICE_INFO, JsonNode.class);
			if (response != null && response.isArray()) {
				JsonNode deviceInfo = response.get(0);
				deviceId = deviceInfo.get("_id").asText();
				for (DeviceInfoEnum item : DeviceInfoEnum.values()) {
					if (deviceInfo.has(item.getField())) {
						cacheValue.put(item.getName(), deviceInfo.get(item.getField()).asText());
					}
				}
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error when retrieving device info", e);
		}
	}

	/**
	 * Populates the provided statistics map with monitoring properties from the cache.
	 *
	 * @param stats a map to be populated with the device monitoring properties.
	 */
	private void populateMonitoringProperties(Map<String, String> stats) {
		for (DeviceInfoEnum item : DeviceInfoEnum.values()) {
			String name = item.getName();
			String value = getDefaultValueForNullData(cacheValue.get(name));
			switch (item) {
				case LAST_CONNECTED:
					stats.put(name, formatMillisecondsToDate(value));
					break;
				case SERIAL_NUMBER:
					stats.put(name, value.replace(HaivisionConstant.SPACE, HaivisionConstant.EMPTY));
					break;
				default:
					stats.put(name, value);
			}
		}
	}

	/**
	 * Retrieves routing information for the device and populates the cache with route details.
	 *
	 * @throws ResourceNotReachableException if an error occurs when retrieving route information.
	 */
	private void retrieveRouteInfo() {
		try {
			JsonNode response = this.doGet(String.format(HaivisionCommand.GET_ALL_ROUTE, deviceId), JsonNode.class);
			if (response != null && response.has(HaivisionConstant.DATA) && response.get(HaivisionConstant.DATA).isArray()) {
				allRouteNameSet.clear();
				for (JsonNode item : response.get(HaivisionConstant.DATA)) {
					String group = item.get(HaivisionConstant.NAME).asText();
					allRouteNameSet.add(group);
					for (RouteInfoEnum routeInfo : RouteInfoEnum.values()) {
						if (item.has(routeInfo.getField())) {
							if (routeInfo.equals(RouteInfoEnum.SOURCE) || routeInfo.equals(RouteInfoEnum.DESTINATION)) {
								cacheValue.put(group + HaivisionConstant.HASH + routeInfo.getName(), getDefaultValueForNullData(item.get(routeInfo.getField()).toString()));

							} else {
								cacheValue.put(group + HaivisionConstant.HASH + routeInfo.getName(), getDefaultValueForNullData(item.get(routeInfo.getField()).asText()));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error when retrieving route info", e);
		}
	}

	/**
	 * Populates the provided statistics map with route information from the cache.
	 *
	 * @param stats a map to be populated with the route information.
	 */
	private void populateRouteInfo(Map<String, String> stats) {
		if (StringUtils.isNullOrEmpty(filterAllRouteName) || HaivisionConstant.FALSE.equalsIgnoreCase(filterAllRouteName)) {
			if (StringUtils.isNullOrEmpty(filterByRouteName)) {
				return;
			} else {
				allRouteNameSet = convertStringToSet(filterByRouteName);
			}
		}
		for (String name : allRouteNameSet) {
			for (RouteInfoEnum item : RouteInfoEnum.values()) {
				String nameProperty = name + HaivisionConstant.HASH + item.getName();
				String value = getDefaultValueForNullData(cacheValue.get(nameProperty));
				switch (item) {
					case SOURCE:
						populateSourceInfo(stats, value, name);
						break;
					case DESTINATION:
						populateDestinationInfo(stats, value, name);
						break;
					case UPTIME:
						stats.put(nameProperty, convertTimeFormat(value));
						break;
					default:
						stats.put(nameProperty, value);
						break;
				}
			}
		}
	}

	/**
	 * Populates the provided statistics map with source information from the given JSON string.
	 *
	 * @param stats a map to be populated with the source information.
	 * @param jsonString a JSON string containing the source information.
	 * @param name the name of the route for which the source information is being populated.
	 */
	private void populateSourceInfo(Map<String, String> stats, String jsonString, String name) {
		if (jsonString.equalsIgnoreCase(HaivisionConstant.NONE)) {
			return;
		}
		try {
			JsonNode node = objectMapper.readTree(jsonString);
			for (RouteConfigurationEnum item : RouteConfigurationEnum.values()) {
				if (!node.has(item.getField())) {
					continue;
				}
				String value = getDefaultValueForNullData(node.get(item.getField()).asText());
				switch (item) {
					case ADDRESS:
						String port = node.get("port").asText();
						stats.put(name + HaivisionConstant.HASH + HaivisionConstant.SOURCE + item.getName(), value + HaivisionConstant.COLON + port);
						break;
					default:
						stats.put(name + HaivisionConstant.HASH + HaivisionConstant.SOURCE + item.getName(), value);
						break;
				}
			}
		} catch (Exception e) {
			logger.error("Error while populate the Source Info", e);
		}
	}

	/**
	 * Populates the provided statistics map with destination information from the given JSON string.
	 *
	 * @param stats a map to be populated with the destination information.
	 * @param jsonString a JSON string containing the destination information.
	 * @param name the name of the route for which the destination information is being populated.
	 */
	private void populateDestinationInfo(Map<String, String> stats, String jsonString, String name) {
		if (jsonString.equalsIgnoreCase(HaivisionConstant.NONE)) {
			return;
		}
		try {
			JsonNode node = objectMapper.readTree(jsonString);
			if (!node.isArray()) {
				return;
			}
			int index = 1;
			for (JsonNode destination : node) {
				String destinationIndex = node.size() == 1 ? HaivisionConstant.EMPTY : String.valueOf(index);
				for (RouteConfigurationEnum item : RouteConfigurationEnum.values()) {
					if (!destination.has(item.getField())) {
						continue;
					}
					String value = getDefaultValueForNullData(destination.get(item.getField()).asText());
					switch (item) {
						case ADDRESS:
							String port = destination.get("port").asText();
							stats.put(name + HaivisionConstant.HASH + HaivisionConstant.DESTINATION + destinationIndex + item.getName(), value + HaivisionConstant.COLON + port);
							break;
						default:
							stats.put(name + HaivisionConstant.HASH + HaivisionConstant.DESTINATION + destinationIndex + item.getName(), value);
							break;
					}
				}
				index++;
			}
		} catch (Exception e) {
			logger.error("Error while populate the Source Info", e);
		}
	}

	/**
	 * Converts a comma-separated string into a set of trimmed strings.
	 *
	 * @param input the comma-separated string to convert.
	 * @return a set containing the trimmed strings.
	 */
	private Set<String> convertStringToSet(String input) {
		return Arrays.stream(input.split(","))
				.map(String::trim)
				.collect(Collectors.toSet());
	}

	/**
	 * Formats a string representing milliseconds into a date string in the format "MMM d, yyyy, h:mm a GMT".
	 *
	 * @param inputValue the string representing milliseconds.
	 * @return the formatted date string, or a default value if the input is "NONE" or invalid.
	 */
	private String formatMillisecondsToDate(String inputValue) {
		if (inputValue.equals(HaivisionConstant.NONE)) {
			return inputValue;
		}
		try {
			long milliseconds = Long.parseLong(inputValue);
			Date date = new Date(milliseconds);
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a");
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			return dateFormat.format(date);
		} catch (Exception e) {
			logger.error("Error when convert date data");
			return HaivisionConstant.NONE;
		}
	}

	/**
	 * Converts a time string in the format "hours:minutes:seconds" into a formatted string representing days, hours, and minutes.
	 *
	 * @param timeStr the time string to convert.
	 * @return the formatted time string, or a default value if the input is "NONE" or invalid.
	 */
	private String convertTimeFormat(String timeStr) {
		if (timeStr.equals(HaivisionConstant.NONE)) {
			return timeStr;
		}
		String[] parts = timeStr.split(HaivisionConstant.COLON);
		if (parts.length < 2) {
			return HaivisionConstant.NONE;
		}
		int hours = Integer.parseInt(parts[0]);
		int minutes = Integer.parseInt(parts[1]);

		int days = hours / 24;
		hours = hours / 60;

		return String.format("%d day(s) %d hour(s) %d minute(s)", days, hours, minutes);
	}

	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	private String getDefaultValueForNullData(String value) {
		return StringUtils.isNotNullOrEmpty(value) && !"null".equalsIgnoreCase(value) ? uppercaseFirstCharacter(value) : HaivisionConstant.NONE;
	}

	/**
	 * capitalize the first character of the string
	 *
	 * @param input input string
	 * @return string after fix
	 */
	private String uppercaseFirstCharacter(String input) {
		char firstChar = input.charAt(0);
		return Character.toUpperCase(firstChar) + input.substring(1);
	}
}
