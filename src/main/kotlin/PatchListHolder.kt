import org.json.JSONException
import org.json.JSONObject

fun String.toJsonObject() = JSONObject(this)

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
