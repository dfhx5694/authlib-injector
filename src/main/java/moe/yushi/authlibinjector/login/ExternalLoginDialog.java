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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class ExternalLoginDialog {
	public static final class LoginCredentials {
		private final String apiUrl;
		private final String username;
		private final char[] password;

		private LoginCredentials(String apiUrl, String username, char[] password) {
			this.apiUrl = apiUrl;
			this.username = username;
			this.password = password;
		}

		public String getApiUrl() {
			return apiUrl;
		}

		public String getUsername() {
			return username;
		}

		char[] getPassword() {
			return password;
		}

		public void clearPassword() {
			Arrays.fill(password, '\0');
		}
	}

	public static Optional<LoginCredentials> prompt(String initialApiUrl) {
		AtomicReference<Optional<LoginCredentials>> result = new AtomicReference<>(Optional.empty());
		runOnEdt(() -> {
			JTextField apiUrlField = new JTextField(initialApiUrl == null ? "" : initialApiUrl, 32);
			JTextField usernameField = new JTextField(32);
			JPasswordField passwordField = new JPasswordField(32);
			JPanel panel = new JPanel(new GridBagLayout());
			addRow(panel, 0, "验证服务器 API 地址:", apiUrlField);
			addRow(panel, 1, "账号（邮箱）:", usernameField);
			addRow(panel, 2, "密码:", passwordField);

			while (true) {
				int option = JOptionPane.showConfirmDialog(null, panel, "外置登录", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (option != JOptionPane.OK_OPTION) {
					return;
				}
				String apiUrl = apiUrlField.getText().trim();
				String username = usernameField.getText().trim();
				char[] password = passwordField.getPassword();
				if (apiUrl.isEmpty() || username.isEmpty() || password.length == 0) {
					Arrays.fill(password, '\0');
					JOptionPane.showMessageDialog(null, "请填写验证服务器、账号和密码。", "外置登录", JOptionPane.WARNING_MESSAGE);
					continue;
				}
				result.set(Optional.of(new LoginCredentials(apiUrl, username, password)));
				return;
			}
		});
		return result.get();
	}

	public static Optional<GameProfile> selectProfile(List<GameProfile> profiles) {
		AtomicReference<Optional<GameProfile>> result = new AtomicReference<>(Optional.empty());
		runOnEdt(() -> {
			JComboBox<GameProfile> profileField = new JComboBox<>(profiles.toArray(new GameProfile[0]));
			JPanel panel = new JPanel(new GridBagLayout());
			addRow(panel, 0, "请选择角色:", profileField);
			int option = JOptionPane.showConfirmDialog(null, panel, "外置登录", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (option == JOptionPane.OK_OPTION) {
				result.set(Optional.ofNullable((GameProfile) profileField.getSelectedItem()));
			}
		});
		return result.get();
	}

	public static boolean confirmInsecureServer(String apiUrl) {
		AtomicReference<Boolean> result = new AtomicReference<>(false);
		runOnEdt(() -> {
			int option = JOptionPane.showConfirmDialog(null,
					"验证服务器使用未加密的 HTTP 连接，账号密码可能被盗。\n确定要继续吗？\n" + apiUrl,
					"安全警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			result.set(option == JOptionPane.YES_OPTION);
		});
		return result.get();
	}

	public static void showError(String message) {
		runOnEdt(() -> JOptionPane.showMessageDialog(null, message, "外置登录失败", JOptionPane.ERROR_MESSAGE));
	}

	private static void addRow(JPanel panel, int row, String label, javax.swing.JComponent field) {
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(4, 4, 4, 8);
		panel.add(new JLabel(label), labelConstraints);

		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = row;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.weightx = 1;
		fieldConstraints.insets = new Insets(4, 4, 4, 4);
		panel.add(field, fieldConstraints);
	}

	private static void runOnEdt(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
			return;
		}
		try {
			SwingUtilities.invokeAndWait(runnable);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for login dialog", e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException("Failed to display login dialog", e.getCause());
		}
	}

	private ExternalLoginDialog() {}
}
