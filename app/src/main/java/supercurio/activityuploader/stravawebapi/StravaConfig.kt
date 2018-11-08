package supercurio.activityuploader.stravawebapi

object StravaConfig {

    const val UPLOAD_PROGRESS_CHECK_MAX_DURATION_S = 60
    const val UPLOAD_PROGRESS_CHECK_INCREMENT_MS = 200L

    val OAUTH_APPROVAL_PROMPT = OauthApprovalPrompt.FORCE
    const val CUSTOM_TAB_COLOR = 0xFC4C02

    enum class OauthApprovalPrompt {
        FORCE,
        AUTO, ;

        override fun toString(): String = super.toString().toLowerCase()
    }

    private val SCOPE_ALL = listOf(
            "read",
            "read_all",
            "profile:read_all",
            "profile:write",
            "activity:read",
            "activity:read_all",
            "activity:write"
    )

    private val SCOPE_REQUIRED = listOf(
            "read",
            "activity:write"
    )

    val SCOPE = SCOPE_REQUIRED

}
