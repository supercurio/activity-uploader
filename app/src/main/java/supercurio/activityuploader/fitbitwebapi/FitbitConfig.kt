package supercurio.activityuploader.fitbitwebapi

object FitbitConfig {

    private val SCOPE_ALL = listOf("activity", "heartrate", "location", "nutrition",
                                   "profile", "settings", "sleep", "social", "weight")

    private val SCOPE_REQUIRED = listOf("heartrate", "profile")

    val SCOPE = SCOPE_REQUIRED
    val PROMPT = Prompt.LOGIN_CONSENT.toString()

    enum class Prompt(private val string: String) {
        NONE("none"),
        CONSENT("consent"),
        LOGIN("login"),
        LOGIN_CONSENT("login consent"), ;

        override fun toString(): String {
            return string
        }
    }
}
