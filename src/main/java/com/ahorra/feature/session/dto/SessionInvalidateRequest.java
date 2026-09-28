package com.ahorra.feature.session.dto;

import com.ahorra.feature.session.web.AdminSessionController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body for POST /admin/sessions/invalidate-by-username - pushed by
 * internal-auth (SyncPointSessionClient) the moment an admin uses
 * "Logout all sessions" there, so this app's own user_sessions row is
 * freed immediately instead of waiting on the next login attempt's
 * lazy /auth/session-status pull.
 *
 * @see AdminSessionController
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionInvalidateRequest {

    /** The internal-auth username, i.e. this app's own User#username - not the phone number. */
    private String username;
}
