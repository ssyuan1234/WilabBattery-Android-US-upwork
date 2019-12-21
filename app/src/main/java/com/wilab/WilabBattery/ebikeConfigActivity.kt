package com.wilab.WilabBattery

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_ebike_config.*
import org.jetbrains.anko.toast

class ebikeConfigActivity : AppCompatActivity() {

    private var powerEbike = 0
    private var velocityEbike = 25
    private var weightEbike = 100
    private var airDragEbike = 29
    private var rollingFrictionEbike = 10
    private var currentEbike = 0
    private var voltageEbike = 0
    private var realpowerEbike = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ebike_config)


        backToProduct.setOnClickListener {
            finish()
        }


        calculate.setOnClickListener {


            //make sure input data is not empty or null
            if (velocity.text.toString() == "" || weight.text.toString() == "") {
                Toast.makeText(this, "重量，速度数据格式不正确", Toast.LENGTH_LONG).show()
                return@setOnClickListener

            } else {
                if (Integer.parseInt(velocity.text.toString()) != null && Integer.parseInt(weight.text.toString()) != null ) {
                    velocityEbike = Integer.parseInt(velocity.text.toString().trim())
                    weightEbike = Integer.parseInt(weight.text.toString().trim())
                    println("$weightEbike, $velocityEbike")


                } else {
                    Toast.makeText(this, "重量，速度数据格式不正确", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            //airDrag = 0.5 * 0.321 * 1.226 * velocity ^2
            airDragEbike = 5 * 321 * 1226 * velocityEbike * velocityEbike / 10 /3600 /3600

            //rollingFriction = 9.8 * weight * friction coefficient (0.03)
            rollingFrictionEbike = weightEbike * 98 * 3/1000

            powerEbike = velocityEbike * 1000 * (airDragEbike + rollingFrictionEbike)/3600

            airdrag.text = "风阻力 = " + airDragEbike.toString() + " 牛顿"
            rollingfriction.text = "摩擦力 = " + rollingFrictionEbike.toString() + " 牛顿"

            power.text = "功率 = " + powerEbike.toString() + " 千瓦"


        }

        realpowercalculate.setOnClickListener {


            if (current.text.toString() == "" || voltage.text.toString() == "") {
                Toast.makeText(this, "电池，电压数据格式不正确", Toast.LENGTH_LONG).show()
                return@setOnClickListener

            } else {

             //   println(current.text.toString())
                if (Integer.parseInt(current.text.toString()) != null && Integer.parseInt(voltage.text.toString()) != null ) {
                    currentEbike = Integer.parseInt(current.text.toString().trim())
              //      println(currentEbike)

                    voltageEbike = Integer.parseInt(voltage.text.toString().trim())

             //       println(currentEbike)

                } else {
                    Toast.makeText(this, "电池，电压数据格式不正确", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }


            if (currentEbike !=null && voltageEbike != null) {
                realpowerEbike = currentEbike * voltageEbike
            } else {
                //toast("电池，电压数据格式不正确")
               // Toast.makeText(this, "You did not enter a username", Toast.LENGTH_SHORT).show();
                Log.d("device", "电池，电压数据格式不正确")


            }

            realpower.text = realpowerEbike.toString() + " 千瓦"
        }

    }
}
