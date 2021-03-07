import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object Constants {
    const val GERRIT_PATCHLIST_QUERY: String =
        "ssh %s gerrit query --dependencies --all-approvals --submit-records --format json %s"
    const val GERRIT_SAMETOPIC_QUERY: String =
        """"ssh %s gerrit query --format json topic:"%s" status:open)"""
}

class GerritSshCommand(private val sshProfile: String) : SshCommand {
    private fun execShellSsh(queryType: String, arg: String): String {
        val cmdArgs = queryType.format(sshProfile, arg)
            .split(" ")
            .toTypedArray()
//      println(cmdArgs.joinToString())
        val process = ProcessBuilder(*cmdArgs)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        val input = BufferedReader(InputStreamReader(process.inputStream))
        process.waitFor(5, TimeUnit.SECONDS)
        return input.readLines().joinToString()
    }

    override operator fun invoke(query: String, arg: String) = execShellSsh(query, arg)
}