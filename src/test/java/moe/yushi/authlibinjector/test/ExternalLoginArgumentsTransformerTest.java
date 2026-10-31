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
package moe.yushi.authlibinjector.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import moe.yushi.authlibinjector.login.AuthenticationResult;
import moe.yushi.authlibinjector.login.GameProfile;
import moe.yushi.authlibinjector.transform.support.ExternalLoginArgumentsTransformer;
import org.junit.jupiter.api.Test;

public class ExternalLoginArgumentsTransformerTest {
	private static final GameProfile PROFILE = new GameProfile("069a79f4444e94726676e2b3f56f366f", "character");
	private static final ExternalLoginArgumentsTransformer TRANSFORMER = new ExternalLoginArgumentsTransformer(
			new AuthenticationResult(PROFILE, "external-token", "{\"preferredLanguage\":[\"zh_CN\"]}"));

	@Test
	public void testReplaceCredentials() {
		String[] original = {
				"--username", "microsoft-user",
				"--uuid", "00000000000000000000000000000000",
				"--accessToken", "microsoft-token",
				"--userProperties", "{}",
				"--userType", "msa",
				"--demo"
		};
		String[] transformed = TRANSFORMER.transform(original);
		assertArrayEquals(new String[] {
				"--username", "character",
				"--uuid", "069a79f4444e94726676e2b3f56f366f",
				"--accessToken", "external-token",
				"--userProperties", "{\"preferredLanguage\":[\"zh_CN\"]}",
				"--userType", "msa",
				"--demo"
		}, transformed);
		assertArrayEquals(new String[] {
				"--username", "microsoft-user",
				"--uuid", "00000000000000000000000000000000",
				"--accessToken", "microsoft-token",
				"--userProperties", "{}",
				"--userType", "msa",
				"--demo"
		}, original);
	}

	@Test
	public void testMissingValuesAreIgnored() {
		assertArrayEquals(new String[] {
				"--username", "--uuid", "--accessToken"
		}, TRANSFORMER.transform(new String[] {
				"--username", "--uuid", "--accessToken"
		}));
	}

	@Test
	public void testRepeatedArgumentsAreReplaced() {
		assertArrayEquals(new String[] {
				"--username", "character",
				"--username", "character"
		}, TRANSFORMER.transform(new String[] {
				"--username", "first",
				"--username", "second"
		}));
	}
}
