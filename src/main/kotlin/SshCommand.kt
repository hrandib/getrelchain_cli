interface SshCommand {
    operator fun invoke(query: String): String
}