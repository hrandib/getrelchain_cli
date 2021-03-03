import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

const val COMMAND_TEMPLATE: String =
    "ssh %s gerrit query --dependencies --submit-records --format json %s"

class GerritSshCommand(private val sshProfile: String) : SshCommand {
    private fun execShellSsh(command: String): String {
        val cmdArgs = COMMAND_TEMPLATE.format(sshProfile, command)
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

    override operator fun invoke(query: String) = execShellSsh(query)
}