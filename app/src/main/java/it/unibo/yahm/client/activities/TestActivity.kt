package it.unibo.yahm.client.activities

import android.os.Bundle
import android.util.Log
import it.unibo.yahm.R
import jaca.android.JaCaService
import jaca.android.dev.JaCaActivity

class TestActivity : JaCaActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        //runJaCaService(R.string.mas2j)
        val url =
            TestActivity::class.java.getResource(this.resources.getString(R.string.mas2j))
        Log.v("TEST", url!!.toString())
    }
}