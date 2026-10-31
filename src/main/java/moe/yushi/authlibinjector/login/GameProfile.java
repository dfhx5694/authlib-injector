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
import static moe.yushi.authlibinjector.util.UUIDUtils.fromUnsignedUUID;
import static moe.yushi.authlibinjector.util.UUIDUtils.toUnsignedUUID;
import java.util.Objects;

public final class GameProfile {
	private final String id;
	private final String name;

	public GameProfile(String id, String name) {
		this.id = toUnsignedUUID(fromUnsignedUUID(requireNonNull(id)));
		this.name = requireNonNull(name);
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Profile name is empty");
		}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof GameProfile)) {
			return false;
		}
		GameProfile other = (GameProfile) obj;
		return id.equals(other.id) && name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	@Override
	public String toString() {
		return name + " (" + id + ")";
	}
}
