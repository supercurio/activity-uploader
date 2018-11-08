package supercurio.activityuploader.gpsconverter

import android.util.Log
import com.sweetzpot.tcxzpot.Serializer
import org.xml.sax.InputSource
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Created by François Simond on 10/19/18.
 * Part of fitbit-hr
 */
class FormattedXmlSerializer(file: File) : Serializer {

    private val writer: BufferedWriter = BufferedWriter(FileWriter(file))
    private val stringBuilder = StringBuilder()

    override fun print(line: String?) {
        stringBuilder.append(line)
    }

    fun save() {
        Log.i(TAG, "save")

        try {

            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            val documentBuilder = documentBuilderFactory.newDocumentBuilder()
            val doc = documentBuilder.parse(InputSource(StringReader(stringBuilder.toString())))

            val tf = TransformerFactory.newInstance().newTransformer()
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            tf.setOutputProperty(OutputKeys.INDENT, "yes")
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1")
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")


            tf.transform(DOMSource(doc), StreamResult(writer))

            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "FormattedXmlSerializer"
    }
}
