package ir.abraham.pdf2md

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start the embedded Python interpreter once
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val output = findViewById<TextView>(R.id.output)
        val pickBtn = findViewById<Button>(R.id.pickBtn)

        val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult

            try {
                // Copy the picked PDF into the app's cache dir so Python can read it by path
                val tempFile = File(cacheDir, "input.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { out -> input.copyTo(out) }
                }

                output.text = "در حال تبدیل..."

                val py = Python.getInstance()
                val module = py.getModule("converter")
                val mdText = module.callAttr("convert", tempFile.absolutePath).toString()

                output.text = mdText
            } catch (e: Exception) {
                Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
                output.text = "تبدیل ناموفق بود."
            }
        }

        pickBtn.setOnClickListener {
            pickFile.launch("application/pdf")
        }
    }
}
