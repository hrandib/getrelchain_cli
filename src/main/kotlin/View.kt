import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import org.json.JSONException
import org.json.JSONObject

enum class ViewType {
    TABLE, RAW
}

fun getView(type: ViewType, list: List<JSONObject>): View {
    return when (type) {
        ViewType.TABLE -> TableView(list)
        ViewType.RAW -> RawView(list)
    }
}

sealed class View(val patchList: List<JSONObject>) {
    abstract fun asString(): String
}

class TableView(list: List<JSONObject>) : View(list) {
    private val excludeTopic: Boolean

    init {
        val firstTopic = getTopic(patchList.first())
        //Exclude if all patches in the list contain the same topic or don't have it
        excludeTopic = patchList.map { getTopic(it) }.all { it == firstTopic }
    }

    override fun asString(): String {
        val t = AsciiTable()
        t.addRule()
        if (excludeTopic) {
            t.addRow("URL", "Subject", "WIP", "TL", "Review", "ML", "Lock")
        } else {
            t.addRow("URL", "Subject", "Topic", "WIP", "TL", "Review", "ML", "Lock")
        }
        t.addRule()
        for (item in patchList) {
            val url = item.getString("url")
                .orEmpty()
                .let { if (excludeTopic) it else shortenUrl(it) }
            val subj = item.getString("subject").orEmpty()
            val wip = getWip(item)
            val topic = getTopic(item)
            val approvals = getApprovals(item)
            var tl = ""
            var cr = ""
            var lock = ""
            var ml = ""
            var sca = ""
            for (approval in approvals) {
                fun getType() = approval.getString("type")
                fun getValue() = approval.getString("value")
                when (getType()) {
                    "Code-Review" -> cr += getValue() + " "
                    "Team-Lead" -> tl += getValue() + " "
                    "Patch-Set-Lock" -> lock += getValue() + " "
                    "Melco-Lead" -> ml += getValue() + " "
                    "SCA" -> sca += getValue() + " "
                }
            }
            if (excludeTopic) {
                t.addRow(url, subj, wip, tl, cr, ml, lock)
            } else {
                t.addRow(url, subj, topic, wip, tl, cr, ml, lock)
            }
        }
        t.addRule()
        t.renderer.cwc = CWC_LongestLine()
        return t.render()
    }

    private fun shortenUrl(url: String): String {
        val patchNumber = url.split("/").last()
        val endIndex = url.indexOf("/c/")
        return if (endIndex != -1) {
            "${url.substring(0, endIndex)}/$patchNumber"
        } else {
            url
        }
    }

    private fun getWip(item: JSONObject): String {
        return try {
            if (item.getBoolean("wip")) {
                "YES"
            } else {
                ""
            }
        } catch (e: JSONException) {
            ""
        }
    }

    private fun getApprovals(item: JSONObject): List<JSONObject> {
        val patchSets = item.getJSONArray("patchSets").filterIsInstance<JSONObject>()
        val approvals = mutableListOf<JSONObject>()
        for (patchSet in patchSets.asReversed()) {
            try {
                approvals.addAll(patchSet.getJSONArray("approvals").filterIsInstance<JSONObject>())
            } catch (e: JSONException) {
                break
            }
            if (patchSet.getString("kind") != "NO_CODE_CHANGE") {
                break
            }
        }
        return approvals
    }

    private fun getTopic(item: JSONObject): String {
        return try {
            item.getString("topic")
        } catch (e: JSONException) {
            ""
        }
    }
}

class RawView(list: List<JSONObject>) : View(list) {
    override fun asString(): String {
        val builder = StringBuilder()
        for (item in patchList) {
            builder.append(item.getString("url"))
                .append(" ; ")
                .append(item.getString("subject"))
                .appendLine()
        }
        return builder.toString()
    }
}