import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import org.json.JSONException
import org.json.JSONObject

enum class ViewType() {
    TABLE, RAW;

    var excludeSubject: Boolean = false

    fun getView(list: List<JSONObject>): View {
        return when (this) {
            TABLE -> TableView(list, excludeSubject)
            RAW -> RawView(list, excludeSubject)
        }
    }
}

sealed class View(val patchList: List<JSONObject>, val excludeSubject: Boolean = false) {
    abstract fun asString(): String
}

class TableView(list: List<JSONObject>, excludeSubject: Boolean) : View(list, excludeSubject) {
    private val excludeTopic: Boolean

    init {
        val firstTopic = getTopic(patchList.first())
        //Exclude if all patches in the list contain the same topic or don't have it
        excludeTopic = patchList.map { getTopic(it) }.all { it == firstTopic }
    }

    override fun asString(): String {
        val t = AsciiTable()
        t.addRule()
        val rowTitle = mutableListOf("URL")
        rowTitle.takeIf { !excludeSubject }?.add("Subject")
        rowTitle.takeIf { !excludeTopic }?.add("Topic")
        rowTitle.addAll(arrayOf("WIP", "TL", "Review", "ML", "Lock"))
        t.addRow(rowTitle)
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
            val rowData = mutableListOf(url)
            rowData.takeIf { !excludeSubject }?.add(subj)
            rowData.takeIf { !excludeTopic }?.add(topic)
            rowData.addAll(arrayOf(wip, tl, cr, ml, lock))
            t.addRow(rowData)
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
        val kindsThatPreserveApprovals = arrayOf("NO_CODE_CHANGE", "TRIVIAL_REBASE", "NO_CHANGE")
        val patchSets = item.getJSONArray("patchSets").filterIsInstance<JSONObject>()
        val approvals = mutableListOf<JSONObject>()
        for (patchSet in patchSets.asReversed()) {
            val mergeNextApproval =
                kindsThatPreserveApprovals.contains(patchSet.getString("kind"))
            try {
                approvals.addAll(
                    patchSet.getJSONArray("approvals")
                        .filterIsInstance<JSONObject>()
                )
            } catch (e: JSONException) {
                if (!mergeNextApproval) break
            }
            if (!mergeNextApproval) break
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

class RawView(list: List<JSONObject>, excludeSubject: Boolean) : View(list, excludeSubject) {
    override fun asString(): String {
        val builder = StringBuilder()
        for (item in patchList.asReversed()) {
            builder.append(item.getString("url"))
            if (!excludeSubject) {
                builder.append(" ; ")
                    .append(item.getString("subject"))
            }
            builder.appendLine()
        }
        return builder.toString()
    }
}