package ir.abraham.pdf2md

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

class MainActivity : ComponentActivity() {

    private var lastMarkdown: String? = null
    private var lastFileNameBase: String = "output"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val output = findViewById<TextView>(R.id.output)
        val pickBtn = findViewById<Button>(R.id.pickBtn)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult

            try {
                val tempFile = File(cacheDir, "input.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { out -> input.copyTo(out) }
                }

                // Try to derive a nice base name from the picked file
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            lastFileNameBase = displayName.substringBeforeLast(".")
                        }
                    }
                }

                output.text = "در حال تبدیل..."
                progressBar.visibility = android.view.View.VISIBLE
                pickBtn.isEnabled = false
                saveBtn.isEnabled = false

                val py = Python.getInstance()
                val module = py.getModule("converter")
                val mdText = module.callAttr("convert", tempFile.absolutePath).toString()

                lastMarkdown = mdText
                output.text = mdText
                saveBtn.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
                output.text = "تبدیل ناموفق بود.\n\n${e.stackTraceToString()}"
            } finally {
                progressBar.visibility = android.view.View.GONE
                pickBtn.isEnabled = true
            }
        }

        val createDoc = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/markdown")
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            val text = lastMarkdown ?: return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray())
                }
                Toast.makeText(this, "فایل ذخیره شد", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "ذخیره ناموفق بود: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        pickBtn.setOnClickListener {
            pickFile.launch("application/pdf")
        }

        saveBtn.setOnClickListener {
            createDoc.launch("$lastFileNameBase.md")
        }
    }
}
