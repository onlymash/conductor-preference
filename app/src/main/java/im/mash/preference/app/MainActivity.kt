package im.mash.preference.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import com.bluelinelabs.conductor.ChangeHandlerFrameLayout
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.RouterTransaction

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val container = findViewById<ChangeHandlerFrameLayout>(R.id.controller_container)
        val router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) router.setRoot(RouterTransaction.with(MyPreferenceController()))
    }
}
