import org.json.JSONException
import org.json.JSONObject

fun String.toJsonObject() = JSONObject(this)

fun main(args: Array<String>) {
    val cmd = SshShellCommand("test")
    val holder = PatchListHolder(cmd, args[0])

    println(holder.list.last().toString(4))
}

class PatchListHolder(val sshCommand: SshCommand, initialPatch: String) {
    var list = mutableListOf<JSONObject>()
    val size: Int
        get() = list.size

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
                (json.getJSONArray("dependsOn")[0] as JSONObject).get("number") as Int
            initPatchList(lowerPatchId)
        } catch(e: JSONException) {
            println("> Finished")
        }
    }
}