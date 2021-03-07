import org.json.JSONException
import org.json.JSONObject

fun String.toJsonObject() = JSONObject(this)

class PatchListHolder(val sshCommand: SshCommand, initialPatch: String) {
    val list = mutableListOf<JSONObject>()
    val topicMap = mutableMapOf<String, MutableList<Int>>()

    fun isEmpty() = list.isEmpty()

    init {
        initPatchList(initialPatch)
        initTopicMap()
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

    private fun initTopicMap() {
        for (item in list) {
            val topic = getTopic(item)
            if (topic.isNotEmpty()) {
                val patchNumber = item.getInt("number");
                if (topicMap.containsKey(topic)) {
                    topicMap[topic]?.add(patchNumber)
                } else {
                    topicMap += topic to mutableListOf(patchNumber)
                }
            }
        }
    }

    private fun getTopic(item: JSONObject): String {
        return try {
            item.getString("topic")
        } catch(e: JSONException) {
            ""
        }
    }
}
