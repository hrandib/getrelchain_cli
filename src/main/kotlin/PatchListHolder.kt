import org.json.JSONException
import org.json.JSONObject

fun String.toJsonObject() = JSONObject(this)

class PatchListHolder(val sshCommand: SshCommand, initialPatch: String) {
    var list = mutableListOf<JSONObject>()

    fun isEmpty() = list.isEmpty()

    init {
        initPatchList(initialPatch)
    }

    private fun initPatchList(patchId: String) {
        fun printFinish() = println("> Finished")
        val json = sshCommand(Constants.GERRIT_PATCHLIST_QUERY, patchId).toJsonObject()
        if (json.getString("status") == "MERGED") {
            printFinish()
            return
        }
        list.add(json)
        print("-")
        try {
            val lowerPatchId =
                (json.getJSONArray("dependsOn")[0] as JSONObject)
                    .getInt("number").toString()
            initPatchList(lowerPatchId)
        } catch (e: JSONException) {
            printFinish()
        }
    }
}
