package com.example.analizer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",
    val userName: String = "",
    val idToken: String = "",
    val projectId: String = "gen-lang-client-0130244093",
    val oauthClientId: String = "391811116027-69jcq39i03ptgirlbi4ekgf2nqj5gbqs.apps.googleusercontent.com",
    val isDriveAuthorized: Boolean = false,
    val scopes: List<String> = listOf("https://www.googleapis.com/auth/drive.readonly")
)

class UserSessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("analyzer_user_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<UserSession> = _session.asStateFlow()

    private fun loadSession(): UserSession {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val idToken = prefs.getString(KEY_ID_TOKEN, "") ?: ""
        val projectId = prefs.getString(KEY_PROJECT_ID, "gen-lang-client-0130244093") ?: "gen-lang-client-0130244093"
        val oauthClientId = prefs.getString(KEY_OAUTH_CLIENT_ID, "391811116027-69jcq39i03ptgirlbi4ekgf2nqj5gbqs.apps.googleusercontent.com")
            ?: "391811116027-69jcq39i03ptgirlbi4ekgf2nqj5gbqs.apps.googleusercontent.com"
        val isDriveAuthorized = prefs.getBoolean(KEY_DRIVE_AUTHORIZED, false)

        return UserSession(
            isLoggedIn = isLoggedIn && email.isNotBlank(),
            userEmail = email,
            userName = name,
            idToken = idToken,
            projectId = projectId,
            oauthClientId = oauthClientId,
            isDriveAuthorized = isDriveAuthorized && email.isNotBlank()
        )
    }

    fun login(email: String, name: String = "", idToken: String = "") {
        val resolvedName = name.ifBlank {
            email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
        }
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, resolvedName)
            .putString(KEY_ID_TOKEN, idToken)
            .putBoolean(KEY_DRIVE_AUTHORIZED, true)
            .apply()

        _session.value = _session.value.copy(
            isLoggedIn = true,
            userEmail = email,
            userName = resolvedName,
            idToken = idToken,
            isDriveAuthorized = true
        )
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_ID_TOKEN)
            .apply()

        _session.value = _session.value.copy(
            isLoggedIn = false,
            idToken = ""
        )
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_OAUTH_CLIENT_ID = "oauth_client_id"
        private const val KEY_DRIVE_AUTHORIZED = "drive_authorized"

        @Volatile
        private var instance: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return instance ?: synchronized(this) {
                instance ?: UserSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
