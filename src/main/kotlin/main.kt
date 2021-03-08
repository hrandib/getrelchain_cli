import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.json.JSONArray
import org.json.JSONObject
import kotlin.system.exitProcess

class CliHandler : CliktCommand(name = "getrelchain", printHelpOnEmptyArgs = true) {
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
        val viewType = if (raw) ViewType.RAW else ViewType.TABLE
        println(getView(viewType, holder.list).asString())
        printSameTopic(viewType, cmd, holder.topicMap)
    }
}

fun printSameTopic(viewType: ViewType, cmd: SshCommand, topicMap: Map<String, List<Int>>) {
    for ((topic, patchList) in topicMap) {
        val jsonString = cmd(Constants.GERRIT_SAMETOPIC_QUERY, topic)
        val sameTopicPatches = JSONArray("[$jsonString]")
            .filterIsInstance<JSONObject>()
            .dropLast(1)
        val withExcludedMainList = sameTopicPatches
            .filter { !patchList.contains(it.getInt("number")) }
        if (withExcludedMainList.isNotEmpty()) {
            if (viewType == ViewType.RAW) {
                print(";")
            }
            println("Common patches with topic \"$topic\":")
            println(getView(viewType, withExcludedMainList).asString())
        }
    }
}

fun main(args: Array<String>) = CliHandler().main(args)
