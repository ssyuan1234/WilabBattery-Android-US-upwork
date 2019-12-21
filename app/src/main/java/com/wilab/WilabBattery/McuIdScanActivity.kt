package com.wilab.WilabBattery

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wilab.WilabBattery.ControlActivity.Companion.m_mcuID_QR
import kotlinx.android.synthetic.main.activity_mcu_id_scan.*

class McuIdScanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mcu_id_scan)



        McuIdBackToNetwork.setOnClickListener {
            finish()
        }

        //To receive data from startActivity
        var bundle = this.intent.extras


        m_mcuID_QR = bundle.get("m_mcuID_QR").toString()


        mcuId.text = m_mcuID_QR
    }
}
