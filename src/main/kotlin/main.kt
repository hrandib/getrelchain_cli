import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlin.system.exitProcess

class CliHandler : CliktCommand(printHelpOnEmptyArgs = true) {
    private val raw by option(
        "-r",
        "--raw",
        help = "Print output in the raw format (Jenkins patch list)"
    ).flag()
    private val sshProfile by argument(help = "SSH config entry (.ssh/config)")
    private val patchId by argument(help = "Top patch ID in the gerrit relation chain")

    override fun run() {
        val cmd = GerritSshCommand(sshProfile)
        val holder = PatchListHolder(cmd, patchId)
        if (holder.isEmpty()) {
            println("Something is going wrong, patch list is empty, nothing to print")
            exitProcess(2)
        }
        val resultView: View = if (raw) {
            RawView(holder.list)
        } else {
            TableView(holder.list)
        }
        println(resultView.asString())
    }
}

fun main(args: Array<String>) = CliHandler().main(args)
