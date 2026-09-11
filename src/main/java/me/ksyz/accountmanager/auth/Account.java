package me.ksyz.accountmanager.auth;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class Account {
    private String refreshToken;
    private String accessToken;
    private String username;
    private long unban;
    private String clientId;
    private String scope;
    private boolean offline;

    // Microsoft 账号
    public Account(
            String refreshToken,
            String accessToken,
            String username,
            String clientId,
            String scope
    ) {
        this(
                refreshToken,
                accessToken,
                username,
                0L,
                clientId,
                scope
        );
    }

    // Offline 账号
    public Account(String username, boolean offline) {
        this.refreshToken = "";
        this.accessToken = "";
        this.username = username;
        this.unban = 0L;
        this.clientId = "";
        this.scope = "";
        this.offline = offline;
    }

    // Microsoft 账号（带 unban）
    public Account(
            String refreshToken,
            String accessToken,
            String username,
            long unban,
            String clientId,
            String scope
    ) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.username = username;
        this.unban = unban;
        this.clientId = clientId;
        this.scope = scope;
        this.offline = false;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public String getClientId() {
        return clientId;
    }

    public String getScope() {
        return scope;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUsername() {
        return username;
    }

    public long getUnban() {
        return unban;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUnban(long unban) {
        this.unban = unban;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
