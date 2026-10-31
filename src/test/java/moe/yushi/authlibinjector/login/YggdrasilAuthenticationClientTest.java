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
import static moe.yushi.authlibinjector.util.IOUtils.asBytes;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonObject;
import static moe.yushi.authlibinjector.util.JsonUtils.parseJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import moe.yushi.authlibinjector.internal.org.json.simple.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class YggdrasilAuthenticationClientTest {
	private static final GameProfile PROFILE_1 = new GameProfile("069a79f4444e94726676e2b3f56f366f", "character1");
	private static final GameProfile PROFILE_2 = new GameProfile("069a79f4444e94726676e2b3f56f3670", "character2");

	private HttpServer server;
	private YggdrasilAuthenticationClient client;
	private final Map<String, String> responses = new HashMap<>();
	private final Map<String, Integer> responseCodes = new HashMap<>();
	private final Map<String, JSONObject> requests = new HashMap<>();

	@BeforeEach
	public void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/yggdrasil/authserver/", this::handle);
		server.start();
		client = new YggdrasilAuthenticationClient("http://127.0.0.1:" + server.getAddress().getPort() + "/api/yggdrasil");
	}

	@AfterEach
	public void stopServer() {
		server.stop(0);
	}

	@Test
	public void testAuthenticateWithSelectedProfile() throws IOException {
		respond("authenticate", "{\"accessToken\":\"token1\",\"clientToken\":\"client1\","
				+ "\"availableProfiles\":[" + profileJson(PROFILE_1) + "],"
				+ "\"selectedProfile\":" + profileJson(PROFILE_1) + ","
				+ "\"user\":{\"properties\":[{\"name\":\"preferredLanguage\",\"value\":\"zh_CN\"}]}}");

		AuthenticationResponse response = client.authenticate("user@example.com", "password".toCharArray());
		AuthenticationResult result = response.toAuthenticationResult();

		assertEquals(PROFILE_1, result.getProfile());
		assertEquals("token1", result.getAccessToken());
		assertEquals("{\"preferredLanguage\":[{\"name\":\"preferredLanguage\",\"value\":\"zh_CN\"}]}", result.getUserProperties());
		JSONObject request = requests.get("authenticate");
		assertEquals("user@example.com", request.get("username"));
		assertEquals("password", request.get("password"));
		assertEquals(true, request.get("requestUser"));
		assertEquals("Minecraft", ((JSONObject) request.get("agent")).get("name"));
		assertFalse(request.get("clientToken").toString().isEmpty());
	}

	@Test
	public void testSelectProfileRefreshesToken() throws IOException {
		respond("authenticate", "{\"accessToken\":\"old-token\",\"clientToken\":\"client1\","
				+ "\"availableProfiles\":[" + profileJson(PROFILE_1) + "," + profileJson(PROFILE_2) + "]}");
		respond("refresh", "{\"accessToken\":\"new-token\",\"clientToken\":\"client1\","
				+ "\"selectedProfile\":" + profileJson(PROFILE_2) + "}");

		AuthenticationResponse response = client.authenticate("user@example.com", "password".toCharArray());
		AuthenticationResult result = client.selectProfile(response, PROFILE_2);

		assertEquals(PROFILE_2, result.getProfile());
		assertEquals("new-token", result.getAccessToken());
		assertEquals("{}", result.getUserProperties());
		JSONObject refresh = requests.get("refresh");
		assertEquals("old-token", refresh.get("accessToken"));
		assertEquals("client1", refresh.get("clientToken"));
		assertEquals(PROFILE_2.getId(), ((JSONObject) refresh.get("selectedProfile")).get("id"));
		assertEquals(PROFILE_2.getName(), ((JSONObject) refresh.get("selectedProfile")).get("name"));
	}

	@Test
	public void testRefreshMustSelectRequestedProfile() throws IOException {
		respond("authenticate", "{\"accessToken\":\"old-token\",\"clientToken\":\"client1\","
				+ "\"availableProfiles\":[" + profileJson(PROFILE_1) + "," + profileJson(PROFILE_2) + "]}");
		respond("refresh", "{\"accessToken\":\"new-token\",\"clientToken\":\"client1\","
				+ "\"selectedProfile\":" + profileJson(PROFILE_1) + "}");

		AuthenticationResponse response = client.authenticate("user@example.com", "password".toCharArray());
		assertThrows(IOException.class, () -> client.selectProfile(response, PROFILE_2));
	}

	@Test
	public void testServerErrorMessage() {
		responseCodes.put("authenticate", 403);
		responses.put("authenticate", "{\"errorMessage\":\"Invalid credentials\"}");

		IOException exception = assertThrows(IOException.class,
				() -> client.authenticate("user@example.com", "secret-password".toCharArray()));
		assertTrue(exception.getMessage().contains("Invalid credentials"));
		assertFalse(exception.getMessage().contains("secret-password"));
	}

	private void respond(String endpoint, String response) {
		responses.put(endpoint, response);
	}

	private void handle(HttpExchange exchange) throws IOException {
		String endpoint = exchange.getRequestURI().getPath().substring("/api/yggdrasil/authserver/".length());
		requests.put(endpoint, asJsonObject(parseJson(new String(asBytes(exchange.getRequestBody()), UTF_8))));
		byte[] response = responses.getOrDefault(endpoint, "{}").getBytes(UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(responseCodes.getOrDefault(endpoint, 200), response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}

	private static String profileJson(GameProfile profile) {
		return "{\"id\":\"" + profile.getId() + "\",\"name\":\"" + profile.getName() + "\"}";
	}
}
