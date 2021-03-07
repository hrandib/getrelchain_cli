import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if(args.isEmpty()) {
        println("Usage:\n\t getrelchain [ssh profile] [patch id] [-r]")
        exitProcess(1)
    }
    val cmd = GerritSshCommand(args[0])
    val holder = PatchListHolder(cmd, args[1])
    if (holder.isEmpty()) {
        println("Something is going wrong, nothing to print")
        exitProcess(2)
    }
    val resultView: View = if (args.size == 3 && args[2] == "-r") {
        RawView(holder.list)
    } else {
        TableView(holder.list)
    }
    println(resultView.asString())
}
