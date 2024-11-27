package org.teamapps.application.server.system.server;

import org.teamapps.application.server.ServerMode;

public class ServerData {

	public static void setServerMode(ServerMode mode) {
		if (mode == null) {
			throw new RuntimeException("ERROR: trying to update invalid server mode");
		}
		if (serverMode != null) {
			throw new RuntimeException("ERROR: trying to update existing server mode");
		}
		serverMode = mode;
	}


	private static void checkServerData() {
		if (serverMode == null) {
			throw new RuntimeException("ERROR: trying to access unset server data");
		}
	}

	private static ServerMode serverMode;

	public static ServerMode getServerMode() {
		checkServerData();
		return serverMode;
	}

	public static boolean isProductionMode() {
		return getServerMode() == ServerMode.PRODUCTION;
	}


}
