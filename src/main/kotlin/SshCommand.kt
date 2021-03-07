interface SshCommand {
    operator fun invoke(query: String, arg: String): String
}