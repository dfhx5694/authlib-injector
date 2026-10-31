/*
 * Copyright (C) 2026  Haowei Wen <yushijinhun@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package moe.yushi.authlibinjector.login;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static moe.yushi.authlibinjector.util.IOUtils.CONTENT_TYPE_JSON;
import static moe.yushi.authlibinjector.util.IOUtils.asBytes;
import static moe.yushi.authlibinjector.util.IOUtils.asString;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonArray;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonObject;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonString;
import static moe.yushi.authlibinjector.util.JsonUtils.parseJson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import moe.yushi.authlibinjector.internal.org.json.simple.JSONArray;
import moe.yushi.authlibinjector.internal.org.json.simple.JSONObject;

public final class YggdrasilAuthenticationClient {
	private static final int TIMEOUT = 15000;

	private final String apiRoot;

	public YggdrasilAuthenticationClient(String apiRoot) {
		requireNonNull(apiRoot);
		this.apiRoot = apiRoot.endsWith("/") ? apiRoot : apiRoot + "/";
	}

	public AuthenticationResponse authenticate(String username, char[] password) throws IOException {
		requireNonNull(username);
		requireNonNull(password);

		JSONObject agent = new JSONObject();
		agent.put("name", "Minecraft");
		agent.put("version", 1);

		JSONObject request = new JSONObject();
		request.put("agent", agent);
		request.put("username", username);
		request.put("password", new String(password));
		request.put("clientToken", UUID.randomUUID().toString());
		request.put("requestUser", true);
		return parseResponse(post("authserver/authenticate", request));
	}

	public AuthenticationResult selectProfile(AuthenticationResponse authentication, GameProfile profile) throws IOException {
		requireNonNull(authentication);
		requireNonNull(profile);
		if (authentication.getSelectedProfile().isPresent()) {
			if (profile.equals(authentication.getSelectedProfile().get())) {
				return authentication.toAuthenticationResult();
			}
			throw new IOException("The access token is already bound to another profile");
		}
		if (!authentication.getAvailableProfiles().contains(profile)) {
			throw new IOException("The selected profile is not available");
		}

		JSONObject selectedProfile = new JSONObject();
		selectedProfile.put("id", profile.getId());
		selectedProfile.put("name", profile.getName());

		JSONObject request = new JSONObject();
		request.put("accessToken", authentication.getAccessToken());
		request.put("clientToken", authentication.getClientToken());
		request.put("selectedProfile", selectedProfile);
		request.put("requestUser", true);

		AuthenticationResponse refreshed = parseResponse(post("authserver/refresh", request));
		if (!profile.equals(refreshed.getSelectedProfile().orElse(null))) {
			throw new IOException("Authentication server selected a different profile");
		}
		return refreshed.toAuthenticationResult();
	}

	private AuthenticationResponse parseResponse(String responseText) throws IOException {
		try {
			JSONObject response = asJsonObject(parseJson(responseText));
			String accessToken = requiredString(response, "accessToken");
			String clientToken = requiredString(response, "clientToken");
			List<GameProfile> availableProfiles = response.containsKey("availableProfiles")
					? asJsonArray(response.get("availableProfiles")).stream()
							.map(it -> parseProfile(asJsonObject(it)))
							.collect(toList())
					: emptyList();
			GameProfile selectedProfile = response.containsKey("selectedProfile")
					? parseProfile(asJsonObject(response.get("selectedProfile")))
					: null;
			return new AuthenticationResponse(accessToken, clientToken, availableProfiles, selectedProfile, parseUserProperties(response));
		} catch (UncheckedIOException e) {
			throw e.getCause();
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid profile in authentication response", e);
		}
	}

	private static GameProfile parseProfile(JSONObject json) throws UncheckedIOException {
		return new GameProfile(asJsonString(json.get("id")), asJsonString(json.get("name")));
	}

	private static String requiredString(JSONObject json, String key) throws IOException {
		String value = asJsonString(json.get(key));
		if (value.isEmpty()) {
			throw new IOException("Authentication response has an empty " + key);
		}
		return value;
	}

	private static String parseUserProperties(JSONObject response) {
		JSONObject result = new JSONObject();
		if (!response.containsKey("user")) {
			return result.toJSONString();
		}
		JSONObject user = asJsonObject(response.get("user"));
		if (!user.containsKey("properties")) {
			return result.toJSONString();
		}
		for (Object element : asJsonArray(user.get("properties"))) {
			JSONObject property = asJsonObject(element);
			String name = asJsonString(property.get("name"));
			JSONArray values = (JSONArray) result.get(name);
			if (values == null) {
				values = new JSONArray();
				result.put(name, values);
			}
			values.add(property);
		}
		return result.toJSONString();
	}

	private String post(String path, JSONObject request) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(apiRoot + path).openConnection();
		try {
			connection.setConnectTimeout(TIMEOUT);
			connection.setReadTimeout(TIMEOUT);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
			connection.setRequestProperty("Accept", "application/json");
			try (OutputStream out = connection.getOutputStream()) {
				out.write(request.toJSONString().getBytes(UTF_8));
			}

			int responseCode = connection.getResponseCode();
			InputStream stream = responseCode >= 200 && responseCode < 300
					? connection.getInputStream()
					: connection.getErrorStream();
			String response = stream == null ? "" : readFully(stream);
			if (responseCode < 200 || responseCode >= 300) {
				throw new IOException(errorMessage(responseCode, response));
			}
			return response;
		} finally {
			connection.disconnect();
		}
	}

	private static String readFully(InputStream stream) throws IOException {
		try (InputStream in = stream) {
			return asString(asBytes(in));
		}
	}

	private static String errorMessage(int responseCode, String response) {
		String message = "Authentication server returned HTTP " + responseCode;
		if (!response.isEmpty()) {
			try {
				JSONObject error = asJsonObject(parseJson(response));
				Object serverMessage = error.get("errorMessage");
				if (serverMessage instanceof String) {
					message += ": " + serverMessage;
				}
			} catch (UncheckedIOException e) {
			}
		}
		return message;
	}
}
