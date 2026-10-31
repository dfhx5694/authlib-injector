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

import static moe.yushi.authlibinjector.util.Logging.log;
import static moe.yushi.authlibinjector.util.Logging.Level.ERROR;
import static moe.yushi.authlibinjector.util.Logging.Level.INFO;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import moe.yushi.authlibinjector.APIMetadata;
import moe.yushi.authlibinjector.InitializationException;
import moe.yushi.authlibinjector.login.ExternalLoginDialog.LoginCredentials;

public final class ExternalLoginController {
	public static AuthenticationResult authenticate(APIMetadata metadata, LoginCredentials credentials) {
		try {
			if (isInsecure(metadata.getApiRoot()) && !ExternalLoginDialog.confirmInsecureServer(metadata.getApiRoot())) {
				log(INFO, "External login cancelled");
				throw new InitializationException();
			}

			YggdrasilAuthenticationClient client = new YggdrasilAuthenticationClient(metadata.getApiRoot());
			AuthenticationResponse response = client.authenticate(credentials.getUsername(), credentials.getPassword());
			Optional<GameProfile> selectedProfile = response.getSelectedProfile();
			if (selectedProfile.isPresent()) {
				AuthenticationResult result = response.toAuthenticationResult();
				logSuccess(result);
				return result;
			}

			List<GameProfile> availableProfiles = response.getAvailableProfiles();
			if (availableProfiles.isEmpty()) {
				throw new IOException("该账号下没有可用角色");
			}
			GameProfile profile;
			if (availableProfiles.size() == 1) {
				profile = availableProfiles.get(0);
			} else {
				Optional<GameProfile> selection = ExternalLoginDialog.selectProfile(availableProfiles);
				if (!selection.isPresent()) {
					log(INFO, "External login cancelled");
					throw new InitializationException();
				}
				profile = selection.get();
			}
			AuthenticationResult result = client.selectProfile(response, profile);
			logSuccess(result);
			return result;
		} catch (IOException e) {
			log(ERROR, "External login failed: " + e.getMessage());
			ExternalLoginDialog.showError(e.getMessage());
			throw new InitializationException(e);
		} finally {
			credentials.clearPassword();
		}
	}

	private static void logSuccess(AuthenticationResult result) {
		log(INFO, "External login succeeded: " + result.getProfile().getName() + " (" + result.getProfile().getId() + ")");
	}

	private static boolean isInsecure(String apiUrl) {
		URI uri = URI.create(apiUrl);
		if (!"http".equalsIgnoreCase(uri.getScheme())) {
			return false;
		}
		String host = uri.getHost();
		return !("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host));
	}

	private ExternalLoginController() {}
}
