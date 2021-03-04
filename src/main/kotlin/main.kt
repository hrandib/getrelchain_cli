import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import org.json.JSONException
import org.json.JSONObject
import kotlin.system.exitProcess


fun String.toJsonObject() = JSONObject(this)

fun main(args: Array<String>) {
    val cmd = GerritSshCommand(args[0])
    val holder = PatchListHolder(cmd, args[1])
    if (holder.isEmpty()) {
        println("Something is going wrong, nothing to print")
        exitProcess(1)
    }
    printTable(holder.list)
}

class PatchListHolder(val sshCommand: SshCommand, initialPatch: String) {
    var list = mutableListOf<JSONObject>()

    fun isEmpty() = list.isEmpty()

    init {
        initPatchList(initialPatch.toInt())
    }

    private fun initPatchList(patchId: Int) {
        fun printFinish() = println("> Finished")
        val json = sshCommand(patchId.toString()).toJsonObject()
        if (json.getString("status") == "MERGED") {
            printFinish()
            return
        }
        list.add(json)
        print("-")
        try {
            val lowerPatchId =
                (json.getJSONArray("dependsOn")[0] as JSONObject).getInt("number")
            initPatchList(lowerPatchId)
        } catch (e: JSONException) {
            printFinish()
        }
    }
}

fun printTable(list: List<JSONObject>) {
    val t = AsciiTable()
    t.addRule()
    t.addRow("URL", "Subject", "TL", "Review", "ML", "Lock")
    t.addRule()
    for (item in list) {
        val url = item.getString("url")
        val subj = item.getString("subject")
        val approvals =
            (item.getJSONArray("patchSets").last() as JSONObject)
                .getJSONArray("approvals")
                .filterNotNull()
                .map { it as JSONObject }

        var tl = ""
        var cr = ""
        var lock = ""
        var ml = ""
        var sca = ""
        for (approval in approvals) {
            fun getType() = approval.getString("type")
            fun getValue() = approval.getString("value")
            when (getType()) {
                "Code Review" -> cr += getValue() + " "
                "Team-Lead" -> tl += getValue() + " "
                "Patch-Set-Lock" -> lock += getValue() + " "
                "Melco-Lead" -> ml += getValue() + " "
                "SCA" -> sca += getValue() + " "
            }
        }
        t.addRow(url, subj, tl, cr, ml, lock)
    }
    t.addRule()
    t.renderer.cwc = CWC_LongestLine()
    println(t.render())
}