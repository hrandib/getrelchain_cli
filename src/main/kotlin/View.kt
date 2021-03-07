import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import org.json.JSONException
import org.json.JSONObject

sealed class View(val patchList: List<JSONObject>) {
    abstract fun asString(): String
}

class TableView(list: List<JSONObject>) : View(list) {
    override fun asString(): String {
        val t = AsciiTable()
        t.addRule()
        t.addRow("URL", "Subject", "TL", "Review", "ML", "Lock")
        t.addRule()
        for (item in patchList) {
            val url = shortenUrl(item.getString("url"))
            val subj = item.getString("subject")
            val approvals = try {
                (item.getJSONArray("patchSets").last() as JSONObject)
                    .getJSONArray("approvals")
                    .filterIsInstance<JSONObject>()
            } catch (e: JSONException) {
                emptyList()
            }
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
            t.addRow(url, subj, tl, cr, ml, lock)
        }
        t.addRule()
        t.renderer.cwc = CWC_LongestLine()
        return t.render()
    }

    private fun shortenUrl(url: String): String {
        val patchNumber = url.split("/").last()
        val gerritSite = url.subSequence(0, url.indexOf("/c/"))
        return "$gerritSite/$patchNumber"
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