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

import static java.util.Objects.requireNonNull;

public final class AuthenticationResult {
	private final GameProfile profile;
	private final String accessToken;
	private final String userProperties;

	public AuthenticationResult(GameProfile profile, String accessToken, String userProperties) {
		this.profile = requireNonNull(profile);
		this.accessToken = requireNonNull(accessToken);
		this.userProperties = requireNonNull(userProperties);
	}

	public GameProfile getProfile() {
		return profile;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getUserProperties() {
		return userProperties;
	}
}
