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
package moe.yushi.authlibinjector.transform.support;

import static java.util.Objects.requireNonNull;
import java.util.HashMap;
import java.util.Map;
import moe.yushi.authlibinjector.login.AuthenticationResult;

public final class ExternalLoginArgumentsTransformer {
	private final AuthenticationResult authentication;

	public ExternalLoginArgumentsTransformer(AuthenticationResult authentication) {
		this.authentication = requireNonNull(authentication);
	}

	public String[] transform(String[] args) {
		Map<String, String> replacements = new HashMap<>();
		replacements.put("--username", authentication.getProfile().getName());
		replacements.put("--uuid", authentication.getProfile().getId());
		replacements.put("--accessToken", authentication.getAccessToken());
		replacements.put("--userProperties", authentication.getUserProperties());

		String[] result = args.clone();
		for (int i = 0; i < result.length - 1; i++) {
			String replacement = replacements.get(result[i]);
			if (replacement != null && !result[i + 1].startsWith("--")) {
				result[++i] = replacement;
			}
		}
		return result;
	}
}
