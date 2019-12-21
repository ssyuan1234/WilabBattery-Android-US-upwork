package com.wilab.WilabBattery

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat.startActivity
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.wilab.WilabBattery.ControlActivity.Companion.m_bluetoothAdapter
import kotlinx.android.synthetic.main.activity_scan_device.*
import org.jetbrains.anko.act
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.SparseArray


class ScanDeviceActivity : AppCompatActivity() {



    var m_bluetoothAdapter: BluetoothAdapter? = null

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var m_Scanning: Boolean = false

    var m_discoveredDevices: ArrayList<BluetoothDevice> = ArrayList()
    var m_deviceNameList: ArrayList<String> = ArrayList()
    var m_deviceIdList: ArrayList<String> = ArrayList()
    var m_MSDList: ArrayList<ByteArray> = ArrayList()


    var deviceID =""

    lateinit var MSD: SparseArray<ByteArray>
    lateinit var scanRecordByte: ByteArray

    lateinit var adapter: ArrayAdapter<String>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_device)


        adapter = ArrayAdapter(this@ScanDeviceActivity, android.R.layout.simple_list_item_1,m_deviceNameList)
        select_device_list.adapter = adapter

        select_device_list.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->

            val device: BluetoothDevice = m_discoveredDevices[position]
            val selectedDeviceAddress: String = device.address



            val selectedDeviceID = m_deviceIdList[position]



            // Set the result with this data, and finish the activity
            // initially I just need to pass device address and I simply use intent. I can sent multile data back as long as
            //they have different variable name.

            val returnIntent = Intent()

            returnIntent.putExtra("mcuID", deviceID)   //send mcuID to Network interface
            returnIntent.putExtra("device_address", selectedDeviceAddress)

            setResult(Activity.RESULT_OK, returnIntent)
            finish()


        }




        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


        scanDevice()

        select_device_refresh.setOnClickListener {

            scanDevice() }

        back.setOnClickListener {
            finish()
        }
    }


    // The following code is similar to that listed in Android developer reference.
    // in reference, bluetoothAdapter.startleScan, the method is deprectated. Instead, method bluetoothLeScanner!!.startScan(scanCallback)

    private fun scanDevice() {

   //     m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()       already has a bluetoothAdapter
        bluetoothLeScanner = m_bluetoothAdapter!!.getBluetoothLeScanner()


        val m_Handler = Handler()

            m_Handler.postDelayed({

                m_Scanning = false
                //       progressBar!!.setVisibility(View.INVISIBLE)
                bluetoothLeScanner!!.stopScan(scanCallback)

            }, 12000)

            m_Scanning = true

            bluetoothLeScanner!!.startScan(scanCallback)

    }


    private val scanCallback = object : ScanCallback() {

        override
        fun onScanResult(callbackType: Int, result: ScanResult) {

            super.onScanResult(callbackType, result)


                if (result.device.name != null) {

                    m_deviceNameList.add("${result.device.name}, ${result.device.toString()}")
                    m_discoveredDevices.add(result.device)
                    adapter.notifyDataSetChanged()

                 //      m_deviceAddressList.add(result.device.address)
                    //   var serviceD = result.scanRecord.serviceData

                     MSD = result.scanRecord.manufacturerSpecificData
                     scanRecordByte = result.scanRecord.bytes


                    //convert scanRecord in byte into String and print out.
                   var st = ""
                    for (b in scanRecordByte) {
                        st = String.format("%02X", b)
                        print(st)
                    }


         //           m_scanRecordByteList.add(st)

        //            println(" ")
         //           println("ok, here is the manufacturer specific data")

                    //the following section is trying to manipulate manufacturer specific data
                    //the MCU ID of controller will be stored in deviceID string.

                    for (i in 0 until MSD.size()) {
                        val key = MSD.keyAt(i)
                        // get the object by the key
                        val obj = MSD.get(key)

                        println("$i, $obj")

                        for (m_b in obj) {
                            deviceID = deviceID + String.format("%02X", m_b)

                        }

                        println(deviceID)


                        //take substring of specific manufacturer data when first overCharge protection failed.
                        val errorOverCharge = deviceID.substring(14,14)
                    }

                    val obj =MSD.get(0)
                    m_MSDList.add(obj)

                    m_deviceIdList.add(deviceID)

                    //hera are typical peripheral device result
                    //ScanResult
                    // {mDevice=EC:FB:EE:F2:C4:F1, mScanRecord=ScanRecord
                    // [mAdvertiseFlags=6, mServiceUuids=[0000fff0-0000-1000-8000-00805f9b34fb], mManufacturerSpecificData={513=[3, 5]},
                    // mServiceData={}, mTxPowerLevel=0, mDeviceName=eBike - Wilab],
                    // mRssi=-72, mTimestampNanos=80517397013742}


                    //this step is to extract manufacturerSpecificData
                    //public ScanResult (BluetoothDevice device,
                    //                int eventType,
                    //                int primaryPhy,
                    //                int secondaryPhy,
                    //                int advertisingSid,
                    //                int txPower,
                    //                int rssi,
                    //                int periodicAdvertisingInterval,
                    //                ScanRecord scanRecord,
                    //                long timestampNanos)

                }

        }




        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("device", "Scanning Failed $errorCode")

        }

    }

    

    //fun for pairedDevice is most likely useless because I will not ask users to pair the device via cellphone.
 /*   private fun pairedDeviceList () {

        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices
        val list: ArrayList<BluetoothDevice> = ArrayList()

        if (!m_pairedDevices.isEmpty()) {
            for (device: BluetoothDevice in m_pairedDevices){
                list.add(device)
                Log.i("device", ""+device)
            }
        } else {
            toast("No paired device found")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,list)
        select_device_list.adapter = adapter
        select_device_list.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->

            val device: BluetoothDevice = list[position]
            val address: String = device.address

            val intent = Intent (this, MainActivity::class.java)
            intent.putExtra(EXTRA_Address, address)
            startActivity(intent)

        }
    }

    */


    //if bluetooth is disabled, notify users

/*    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BLUETOOTH){
            if (resultCode == Activity.RESULT_OK){
                if (m_bluetoothAdapter!!.isEnabled){
                    toast("Bluetooth has been enabled")
                }else{
                    toast("Bluetooth has been disabled")
                }
            }else{ if (resultCode == Activity.RESULT_CANCELED){
                toast("Bluetooth enabling has been cancled")
            }

            }
        }

    } */


}
