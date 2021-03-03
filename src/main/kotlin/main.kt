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
    for (item in holder.list) {
        val url = item.getString("url")
        val subj = item.getString("subject")
        val submitRecord = item.getJSONArray("submitRecords")[0] as JSONObject
        val status = submitRecord.getString("status")
        println("$url  $subj  $status")
    }
}

class PatchListHolder(val sshCommand: SshCommand, initialPatch: String) {
    var list = mutableListOf<JSONObject>()
    val size: Int
        get() = list.size

    fun isEmpty() = list.isEmpty()

    init {
        initPatchList(initialPatch.toInt())
    }

    operator fun get(i: Int): JSONObject {
        return list[i]
    }

    private fun initPatchList(patchId: Int) {
        val json = sshCommand(patchId.toString())
            .toJsonObject()
        list.add(json)
        print("-")
        try {
            val lowerPatchId =
                (json.getJSONArray("dependsOn")[0] as JSONObject).getInt("number")
            initPatchList(lowerPatchId)
        } catch (e: JSONException) {
            println("> Finished")
        }
    }
}