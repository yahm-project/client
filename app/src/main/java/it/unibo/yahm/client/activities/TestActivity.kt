package it.unibo.yahm.client.activities

import it.unibo.pslab.jaca_android.core.ActivityArtifact
import it.unibo.pslab.jaca_android.core.JaCaBaseActivity
import it.unibo.yahm.R

class TestActivity : ActivityArtifact() {
    class BohActivity : JaCaBaseActivity(){}
    fun init() {
        super.init(
            BohActivity::class.java,
            R.layout.activity_test,
            true
        )
    }
}