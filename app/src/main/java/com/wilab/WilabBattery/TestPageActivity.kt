package com.wilab.WilabBattery

import android.bluetooth.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.wilab.WilabBattery.ControlActivity.Companion.m_address
import com.wilab.WilabBattery.ControlActivity.Companion.m_bluetoothAdapter
import kotlinx.android.synthetic.main.activity_ebike_config.*
import kotlinx.android.synthetic.main.activity_test_page.*
import java.io.File
import java.util.*

class TestPageActivity : AppCompatActivity() {

    var preStoredDeviceAddress = ""
    lateinit var mdevice: BluetoothDevice
    lateinit var mBluetoothGatt: BluetoothGatt
    lateinit var m_write_characteristic: BluetoothGattCharacteristic

    val uuid_service_wilab_device = "0000fff0-0000-1000-8000-00805f9b34fb"
    val uuid_characteristic_wilab_device = "0000fff1-0000-1000-8000-00805f9b34fb"
    val uuid_write_characteristic_wilab_device = "0000fff2-0000-1000-8000-00805f9b34fb"

    lateinit var commandString: String





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_page)



        sendInstruction.setOnClickListener {

            if (instruction.text.toString() == "") {
                Toast.makeText(this, "指令格式不正确", Toast.LENGTH_LONG).show()
                return@setOnClickListener

            } else {

                commandString = instruction.text.toString()

               sendStringInstruction(commandString)

            }

        }


        TestPageBackToProduct.setOnClickListener {
            finish()
        }


    }


        fun sendStringInstruction(commandString: String) {

            val filePath = this.getFilesDir().path.toString() + "/deviceAddress.txt"


            //if device address is available, the BLE device is connected automatically
            if (File(filePath).exists()) {

                preStoredDeviceAddress = File(filePath).readText()

                m_address = preStoredDeviceAddress

                m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                mdevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)


                mdevice.connectGatt(this, false, mGattCallback)


                //    val device = mBluetoothAdapter.getRemoteDevice(preStoredDeviceAddress);
                //    device.connectGatt(this, false, gattCallback);

            } else {

                Toast.makeText(this, "蓝牙需要预先链接", Toast.LENGTH_LONG).show()

            }

        }





    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)




            mBluetoothGatt = gatt!!


            if (newState === BluetoothProfile.STATE_CONNECTED) {
                println("Device connected")

                Log.d("BLE", "Device connected")

                Handler(Looper.getMainLooper()).post {
                    val ans = mBluetoothGatt!!.discoverServices()

                    Log.d("BLE", "Discover Services started: $ans")
                }
            }


        }


        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)


            if (status == BluetoothGatt.GATT_SUCCESS) {



                    m_write_characteristic =
                        gatt!!.getService(UUID.fromString(uuid_service_wilab_device))
                            .getCharacteristic(
                                UUID.fromString(
                                    uuid_write_characteristic_wilab_device
                                )
                            )

                    val mm_write_properties = m_write_characteristic?.properties

                    if (mm_write_properties!! * BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {

                        // writing characteristic functions


                        var strBytes = commandString.toByteArray()

                        m_write_characteristic?.getValue()
                        m_write_characteristic?.setValue(strBytes)

                        gatt!!.writeCharacteristic(m_write_characteristic)

                    }



            } else {
                Log.d("BLEconnection", "onServicesDiscovered received: " + status)

            }


        }

    }

}
