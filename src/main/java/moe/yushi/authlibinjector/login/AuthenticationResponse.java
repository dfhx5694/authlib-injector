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

import static java.util.Collections.unmodifiableList;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

final class AuthenticationResponse {
	private final String accessToken;
	private final String clientToken;
	private final List<GameProfile> availableProfiles;
	private final GameProfile selectedProfile;
	private final String userProperties;

	AuthenticationResponse(String accessToken, String clientToken, List<GameProfile> availableProfiles, GameProfile selectedProfile, String userProperties) {
		this.accessToken = accessToken;
		this.clientToken = clientToken;
		this.availableProfiles = unmodifiableList(availableProfiles);
		this.selectedProfile = selectedProfile;
		this.userProperties = userProperties;
	}

	String getAccessToken() {
		return accessToken;
	}

	String getClientToken() {
		return clientToken;
	}

	List<GameProfile> getAvailableProfiles() {
		return availableProfiles;
	}

	Optional<GameProfile> getSelectedProfile() {
		return Optional.ofNullable(selectedProfile);
	}

	AuthenticationResult toAuthenticationResult() throws IOException {
		if (selectedProfile == null) {
			throw new IOException("Authentication server did not select a profile");
		}
		return new AuthenticationResult(selectedProfile, accessToken, userProperties);
	}
}
