package com.wilab.WilabBattery


import android.app.Activity
import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.jar.Manifest
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.wilab.WilabBattery.FragmentMap.Companion.TAG
import java.lang.reflect.Array
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.content.pm.PackageManager
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.*
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import android.text.TextUtils.indexOf
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wilab.WilabBattery.ControlActivity.Companion.m_address
import com.wilab.WilabBattery.ControlActivity.Companion.m_bluetoothAdapter
import com.wilab.WilabBattery.ControlActivity.Companion.m_mcuID
import kotlinx.android.synthetic.main.fragment_fragment_network.*
import kotlinx.android.synthetic.main.fragment_fragment_system.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream
import java.time.Year




class FragmentNetwork : Fragment() {

    //introduce recyclerview into FragmentNetwork
    private lateinit var layoutManager: LinearLayoutManager


    private var m_bluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    private var m_characteristic: BluetoothGattCharacteristic? = null


    val uuid_service_wilab_device = "0000fff0-0000-1000-8000-00805f9b34fb"
    val uuid_characteristic_wilab_device = "0000fff1-0000-1000-8000-00805f9b34fb"
    val uuid_write_characteristic_wilab_device = "0000fff2-0000-1000-8000-00805f9b34fb"


    //array for data displayed on the right, 4 voltage data point and temperature and SOC
    private lateinit var statusUpdate: TextView

    private var voltageListVM = arrayOf(String)

    var voltageList = arrayOfNulls<String>(20)                     //List
    private lateinit var listVoltageView: ListView                                      //ListView
    private lateinit var adapterVoltage: ArrayAdapter<String?>                          //ArrayAdapter




    //array for data displayed on the left, 8 voltage data points
    var voltageListLeft = arrayOfNulls<String>(20)                  //List
    private lateinit var listVoltageViewLeft: ListView                                  //ListView
    private lateinit var adapterVoltageLeft: ArrayAdapter<String?>                      //ArrayAdapter

    var errorCode = arrayOfNulls<Int>(8)


    //these variables with "left" simply means the data shown in Android on the left side of the screen.
    private lateinit var jointString: String
    private lateinit var jointStringLeft: String


    //set up message code for message handler

    val UPDATE_TEXT = 1
    val bluetooth_found = 2
    val connecting = 3
    val scan_failed = 4
    val connection_done = 5

    //parameters for update file
    var previousTime: Long = 0
    var fileWrite_Firsttime = 1





    //The following code is message handler to update data and status.
    /*       handleMessage() defines the operations to perform when
      the Handler receives a new Message to process.                     */


    val handler: Handler = object : Handler(Looper.getMainLooper()) {

        override fun handleMessage(inputMessage: Message) {

            // Gets the image task from the incoming Message object.
            if (inputMessage.what == 1) {

                statusUpdate.blink(3)




                adapterVoltage.notifyDataSetChanged()
                adapterVoltageLeft.notifyDataSetChanged()


                //this file path has to be defined in handle message section. Otherwise, program crash
                val filePath = context!!.getFilesDir().path.toString() + "/fileName.txt"

                //this section is used to write data to the file
                if (((SystemClock.elapsedRealtime() - previousTime) > 600000) ||  (fileWrite_Firsttime == 1)) {


                    //Every hour write into data file
                    previousTime = SystemClock.elapsedRealtime()

                    jointString = voltageList.joinToString (separator = ",") {it -> "$it"}
                    jointStringLeft = voltageListLeft.joinToString (separator = ",") {it -> "$it"}

                    //This is writing time stamp and data into the system

                    File(filePath).appendText("${SystemClock.elapsedRealtime()}, $jointString, $jointStringLeft\n")

                    fileWrite_Firsttime = 0

                }



            }

            if (inputMessage.what == 2) {

                statusUpdate.text = "找到蓝牙"

            }

            if (inputMessage.what == 3) {

                statusUpdate.text = "链接进行中"

            }

            if (inputMessage.what == 4) {

                statusUpdate.text = "搜索失败，重按搜索"

            }

            if (inputMessage.what == 5) {

                statusUpdate.text = "链接建立，读取数据"

            }

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }


    //this code sets up the view for this page
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        val rootView = inflater.inflate(R.layout.fragment_fragment_network, container, false)

        val scanDevice = rootView.findViewById<Button>(R.id.scandevice)

        val dataUpdate = rootView.findViewById<Button>(R.id.dataupdate)



        statusUpdate = rootView.findViewById<TextView>(R.id.statusupdate)

        listVoltageView = rootView.findViewById<ListView>(R.id.listvoltagesecondfour)                       //ListView to UI element

        listVoltageViewLeft = rootView.findViewById<ListView>(R.id.listvoltagefirsteight)                 //ListView to UI element




        //recycleview is introduced to have cell data reported in different fragment. It turns out that is not necessary.
        //with BLE connected, the callback can be handled in a separate fragment.

   /*      recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView)                              //ListView to UI element
         recyclerView.layoutManager = LinearLayoutManager(context!!)

         recyclerViewAdapter = VoltageAdapter(voltageListLeft)
        recyclerView.adapter = recyclerViewAdapter           */



        adapterVoltage = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, voltageList)
        adapterVoltageLeft = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, voltageListLeft)

        //initialization of voltageList and voltageListLeft
    // ********The attempt to use listview in arraylist to show voltage turns out to be an utter failure. ****************//
        //Arraylist can not be indexed. But array can be.


        for (i in 0 until 20) { voltageListLeft[i] = "0.0" }
           for (i in 0 until 20) { voltageList[i] = "0.0" }
                for (i in 0 until 8) { errorCode[i] = 0 }


        listVoltageView.adapter = adapterVoltage
        listVoltageViewLeft.adapter = adapterVoltageLeft




         adapterVoltage.notifyDataSetChanged()
         adapterVoltageLeft.notifyDataSetChanged()

   //        recyclerViewAdapter.notifyDataSetChanged()





        //this section is to test writting data to file and store it in internal storage, initializing file
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val month = Calendar.getInstance().get(Calendar.MONTH)
        val day = Calendar.getInstance().get(Calendar.DATE)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val minute = Calendar.getInstance().get(Calendar.MINUTE)
        val second = Calendar.getInstance().get(Calendar.SECOND)


        val filePath = context!!.getFilesDir().path.toString() + "/fileName.txt"
        File(filePath).writeText("$year，$month， $day, $hour，$minute，$second\n")


            //upon clicking on update button, switch to system fragment page and update show deviceID
             dataUpdate.setOnClickListener {


                 val filePathMcuID = context!!.getFilesDir().path.toString() + "/mcuID.txt"
                 m_mcuID = File(filePathMcuID).readText()

                val intent = Intent(activity, McuIdScanActivity::class.java)

                 val bundle = Bundle()
                 bundle.putString("m_mcuID_QR", m_mcuID)        //send mcuID to QR scan page
                 intent.putExtras(bundle)

                 startActivityForResult(intent, 2, Bundle())


                  }



            //This code is for action when "搜索蓝牙电池包" is clicked.
          scanDevice.setOnClickListener {

            //this section is to start new ScanDeviceActivity and come back with selected bluetooth addr3ess\
            //here I introduce bundle(), because I have multiple data to pass between ScanDeviceActivity and FragmentNetwork.


            val intent = Intent(activity, ScanDeviceActivity::class.java)
            startActivityForResult(intent, 1)

              //requestCode "1" is arbitrary， send the action to ScanDeviceActivity class

          }




        //button is name after BLE disconnect. It is used for individual cell info.
            val bleDisconnect = rootView.findViewById<Button>(R.id.disconnect)

            bleDisconnect.setOnClickListener {

                //I have tried many method to access individual cell data. Here is the I to connect again.
                val filePath = context!!.getFilesDir().path.toString() + "/deviceAddress.txt"

                //if device address is available, the BLE device is connected automatically

                 val   preStoredDeviceAddress = File(filePath).readText()

                    val m_address = preStoredDeviceAddress

                             m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val       mdevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)


                            mdevice.connectGatt(requireActivity(), false, mGattCallback)












                    //I have tried to access the data in view model. The problem is that, the data in viewmodel could be changing
                //when I tried to access them.

/*                val viewModel = ViewModelProvider(this@FragmentNetwork).get(SharedViewModel::class.java)

                recyclerView.apply {
                    // set a LinearLayoutManager to handle Android
                    // RecyclerView behavior
                    layoutManager = LinearLayoutManager(context!!)
                    // set the custom adapter to the RecyclerView
                    var recyclerViewAdapter = VoltageAdapter(viewModel.putVoltageLeft())

                    recyclerView.adapter = recyclerViewAdapter

                }   */







                //           var voltageListLeft = arrayOfNulls<String>(16)

   //             for (i in 0 until 15) {

   //                 voltageListLeft[i] = viewModel.putVoltageLeft()[i]
   //             }








       //         if (mBluetoothGatt != null) {
       //             mBluetoothGatt!!.disconnect()
       //         }
            }



        return rootView
    }



    companion object {
        fun newInstance(): FragmentNetwork = FragmentNetwork()
    }



    //blink function, use the blinking to indicate data update, used in message handler to update data and status

    fun View.blink(
        times: Int = Animation.INFINITE,
        duration: Long = 500L,
        offset: Long = 20L,
        minAlpha: Float = 0.0f,
        maxAlpha: Float = 1.0f,
        repeatMode: Int = Animation.REVERSE
    ) {
        startAnimation(AlphaAnimation(minAlpha, maxAlpha).also {
            it.duration = duration
            it.startOffset = offset
            it.repeatMode = repeatMode
            it.repeatCount = times
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        //after switching back to network page, the votlage data is not updated. This is try to update voltage data on UI.

        adapterVoltage.notifyDataSetChanged()
  //      adapterVoltageLeft.notifyDataSetChanged()


        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {

                m_mcuID = data!!.getStringExtra(("mcuID"))
                val m_mcuID_file = m_mcuID + "\n"                //create string for file operation


                m_address = data!!.getStringExtra("device_address")

                val filePath = context!!.getFilesDir().path.toString() + "/fileName.txt"
                File(filePath).appendText(m_mcuID_file)

                //put mcuID into a file
                val filePathMcuID = context!!.getFilesDir().path.toString() + "/mcuID.txt"
                File(filePathMcuID).writeText(m_mcuID)

                //add device address into a file named "deviceAddress.txt"
                val filePathDeviceAddress = context!!.getFilesDir().path.toString() + "/deviceAddress.txt"
                if (m_address != null) {
                File(filePathDeviceAddress).writeText(m_address!!)}


                m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                val mdevice: BluetoothDevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)


                mdevice.connectGatt(requireActivity(), false, mGattCallback)


            } else {
                    //to be decided what to do?

            }
        }

    }




     override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("address", m_address)





    }

       //the function of the following code is
        override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState);




   /*        val viewModel = ViewModelProvider(this@FragmentNetwork).get(SharedViewModel::class.java)

           viewModel.voltageListViewModel.observe(this@FragmentNetwork, androidx.lifecycle.Observer{


               var recyclerViewAdapter = VoltageAdapter(viewModel.putVoltageLeft())
               recyclerView.adapter = recyclerViewAdapter

           })       */






           if (savedInstanceState != null) {
            //probably orientation change
            m_address = savedInstanceState.getString("address")


            //this section is for save the address of the device and start connection.
0
            m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val mdevice: BluetoothDevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)
            mdevice.connectGatt(requireActivity(), false, mGattCallback)


        } else {
            if (m_address != null) {
                //returning from backstack, data is fine, do nothing
            } else {
                //newly created, compute data
                //to be decided
            }
        }
    }




    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

   /*         var intentAction: String?
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED

            } else {
                intentAction = ACTION_GATT_DISCONNECTED
            }
                          */


            handler.obtainMessage(connecting).apply { sendToTarget() }

            mBluetoothGatt = gatt


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


     //           m_bluetoothService = gatt!!.getService(UUID.fromString(uuid_service_wilab_device))


                m_characteristic = gatt!!.getService(UUID.fromString(uuid_service_wilab_device))
                    .getCharacteristic(UUID.fromString(uuid_characteristic_wilab_device))



                val mm_properties = gatt!!.getService(UUID.fromString(uuid_service_wilab_device))
                    .getCharacteristic(UUID.fromString(uuid_characteristic_wilab_device)).properties

                if ((mm_properties * BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    gatt!!.setCharacteristicNotification(
                        gatt!!.getService(UUID.fromString(uuid_service_wilab_device)).getCharacteristic(
                            UUID.fromString(uuid_characteristic_wilab_device)
                        ), true

                    )
                }



                gatt!!.readCharacteristic(m_characteristic)


                  //     println(m_characteristic.uuid.toString())
                //       println(gatt!!.getService(UUID.fromString(uuid_service_wilab_device)).uuid.toString())
                //    val mm_characteristic: BluetoothGattCharacteristic = mm_service.characteristics[0]


                /*       val mm_service_uuid: UUID = gatt!!.services[3].uuid
                val mm_characteristic_uuid: UUID = gatt!!.services[3].characteristics[0].uuid

                val mm_service: BluetoothGattService = gatt!!.getService(mm_service_uuid)
                val mm_characteristic: BluetoothGattCharacteristic = mm_service.getCharacteristic(mm_characteristic_uuid)

                //the following code is siimilar to that in IOS code, check characteristic "notify" properties
                val mm_properties = mm_characteristic.properties
                if ((mm_properties * BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ){
                    gatt!!.setCharacteristicNotification(mm_characteristic, true)}

                    //very important line to find out descriptor's UUID
      //              for (descriptor in mm_characteristic.getDescriptors()) {
          //              Log.e(TAG, "BluetoothGattDescriptor: " + descriptor.getUuid().toString() + descriptor.characteristic.uuid.toString())
          //          }

                    val  mm_descriptor = mm_characteristic.getDescriptor(UUID.fromString(uuid_descriptor_wilab_device))
                    mm_descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    gatt!!.writeDescriptor(mm_descriptor)                                        */


            } else {
                Log.d("BLEconnection", "onServicesDiscovered received: " + status)

            }


        }


        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)

            

            if (characteristic == null) {
                println("characteristic is null")
            }

            handler.obtainMessage(connection_done).apply { sendToTarget() }

            val data = characteristic!!.value


            val dataString: String = Arrays.toString(data)

            println(dataString)


            val startText: Int = 2

        /*    if ( data[0] == null) {
                println("data is null")
            }   */

            if (startText.toByte() == data[0]) {


         //       voltageFirstEight = data

                println("1st half")

                println(Arrays.toString(data))


                voltageListLeft[0] =  "#11,  " + ((data[4].toInt()*100 + data[5].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageListLeft[1] =  "#12,  " + ((data[6].toInt()*100 + data[7].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageListLeft[2] =  "#13,  " + ((data[8].toInt()*100 + data[9].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageListLeft[3] =  "#14,  " + ((data[10].toInt()*100 + data[11].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageListLeft[4] =  "#21,  " + ((data[12].toInt()*100 + data[13].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageListLeft[5] =  "#22,  " + ((data[14].toInt()*100 + data[15].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageListLeft[6] =  "#23,  " + ((data[16].toInt()*100 + data[17].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageListLeft[7] =  "#24,  " + ((data[18].toInt()*100 + data[19].toInt()).toFloat()/1000 + 0.00001).toString().take(5)



                voltageList[4] = "电流,  "  + ((data[3].toInt()*100).toFloat()/100).toString()


                voltageList[5] = ((data[2].toInt()*100).toFloat()/100).toString() + "%"



                handler.obtainMessage(UPDATE_TEXT).apply { sendToTarget() }



            } else {





     //          voltageSecondFour = data

                println("2nd half")
                println(Arrays.toString(data))


                //Array<String> element value assignment
                voltageList[0] = "#31,  " + ((data[0].toInt()*100 + data[1].toInt()).toFloat()/1000 + 0.00001).toString().take(5)
                voltageList[1] = "#32,  " + ((data[2].toInt()*100 + data[3].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageList[2] = "#33,  " + ((data[4].toInt()*100 + data[5].toInt()).toFloat()/1000 + 0.00001).toString().take(5)
                voltageList[3] = "#34,  " + ((data[6].toInt()*100 + data[7].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageList[6] = "温度#1,  " + (((data[8].toInt()*100)-4000).toFloat()/100).toString()
                voltageList[7] = "温度#2,  " + (((data[8].toInt()*100)-4000).toFloat()/100).toString()
                voltageList[8] = "温度#3,  " + (((data[8].toInt()*100)-4000).toFloat()/100).toString()


          //          println("data[9] = $data[9]")

                errorCode[0]  = (data[11].toInt() shr 7)  and 0x01        //convert error byte to int, read the first bit
                errorCode[1]  = (data[11].toInt() shr 6)  and 0x01
                errorCode[2]  = (data[11].toInt() shr 5)  and 0x01
                errorCode[3]  = (data[11].toInt() shr 4)  and 0x01
                errorCode[4]  = (data[11].toInt() shr 3)  and 0x01
                errorCode[5]  = (data[11].toInt() shr 2)  and 0x01
                errorCode[6]  = (data[11].toInt() shr 1)  and 0x01
                errorCode[7]  = (data[11].toInt() shr 0)  and 0x01


                voltageList[9] = "通讯异常   " + errorCode[0].toString()
                voltageList[10] = "过流标志   " + errorCode[1].toString()
                voltageList[11] = "过温标志   " + errorCode[2].toString()
                voltageList[12] = "过充标志   " + errorCode[3].toString()
                voltageList[13] = "过放标志   " + errorCode[4].toString()
                voltageList[14] = "充电标志   " + errorCode[5].toString()
                voltageList[15] = "放电标志   " + errorCode[6].toString()


    /*            println(errorCode[0])
                println(errorCode[1])
                println(errorCode[2])
                println(errorCode[3])
                println(errorCode[4])
                println(errorCode[5])
                println(errorCode[6])
                println(errorCode[7])     */









                handler.obtainMessage(UPDATE_TEXT).apply { sendToTarget() }







                //       println(voltageList[0])


            }







   //         handler.obtainMessage()?.apply {
    //            sendToTarget()
    //        }


        }


        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {

            super.onCharacteristicRead(gatt, characteristic, status)



            if (characteristic == null) {
                println("characteristic is null")
            }

        }

        }



}



//scan result callback。 This code is not longer called. Instead, similar code is called in ScanDevice Activity.

/*   private val scanCallback = object : ScanCallback() {

       override
       fun onScanResult(callbackType: Int, result: ScanResult) {

           super.onScanResult(callbackType, result)


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


        //   println(result.scanRecord.manufacturerSpecificData.toString())

           if ((result.scanRecord != null) && (result.device.name == "eBike - Wilab")) {
     //          println(result.device.name)

                           //    result.scan

                              println(result.scanRecord.serviceData)
                              println(result.scanRecord.serviceUuids)





           }


           //the following to compile the list of devices

           if (result.device in m_bluetoothDeviceList) {
               return
           } else {


               //this logic just add one "未名蓝牙器件" 到 arraylist

               if (result.device.name == null) {


                   //              println("未名蓝牙器件")


               } else {
                   m_deviceNameListNew.add("${result.device.name}, ${result.device.toString()}")

                   m_deviceNameList.add("${result.device.name}, ${result.device.toString()}")
                   m_bluetoothDeviceList.add(result.device)
                   m_deviceAddressList.add(result.device.address)



                   //this code is added to connect to the firt ebike device found during scanning.
                   //if this auto-connection failed, user can still use readDeviceInfo to connect to another


                   if (result.device.name == "eBike - Wilab") {

                       handler.obtainMessage(bluetooth_found).apply { sendToTarget() }

                       when (noEbikeDevice == 0) {
                           true -> {
                               //            m_bluetoothDevice = result.getDevice()
                               noEbikeDevice++

                               result.getDevice().connectGatt(requireActivity(), false, mGattCallback)
                           }

                           false -> {
                               return
                           }

                       }


                   }


               }


               //        get the device to connect
               //        m_bluetoothDevice = result.getDevice()


           }

       }


   }            */


//Device listing is switch a ScanDeviceActivity. After devie is selected, the data is passed to FragmentNetwork and then DeviceInfo is readed.
//the following fun will be  different from readDeviceInfo.


//this sectin of code is not used.
/*   private fun readDeviceInfo(position: Int) {

       if (m_Scanning == false) {

           var mdevice: BluetoothDevice?

           mdevice = m_bluetoothDeviceList[position]

           mdevice.connectGatt(requireActivity(), false, mGattCallback)


       }
   }                         */






/*          val manager = getFragmentManager()


                     val transaction = manager!!.beginTransaction()
                     val fragement = FragmentSystem()

                     val bundle = Bundle()
                     bundle.putString("edttext", "From Activity")
                     //data "from activity" is tranfer to "edttext"
                     // set Fragmentclass Arguments


                     fragement.setArguments(bundle)
                     //use bundle to transfer data from one fragment to another


                     transaction.replace(R.id.fragmentholder, fragement)


                     transaction.addToBackStack(null)
                     transaction.commit()                   */







/*            val manager = getFragmentManager()


             val transaction = manager!!.beginTransaction()
             val fragement = FragmentSystem()

             val bundle = Bundle()
             bundle.putString("edttext", "From Activity")
             //data "from activity" is tranfer to "edttext"
             // set Fragmentclass Arguments


             fragement.setArguments(bundle)
             //use bundle to transfer data from one fragment to another


             transaction.replace(R.id.fragmentholder, fragement)


             transaction.addToBackStack(null)
             transaction.commit()                           */



/*        if (status == BluetoothGatt.GATT_SUCCESS){
      val data = characteristic!!.value

      val dataString: String = Arrays.toString(data)

      val startText: Int = 2

      if ( startText.toByte() == data[0]) {

          println("1st half")

//          voltageFirstEight = data

      }else {

          println("2nd half")

          //         voltageSecondFour = data
 //         println(voltageSecondFour)

      }

  }            */


/*     Thread(Runnable {
    // performing some dummy time taking operation
    var i=0;
    while(i<Int.MAX_VALUE){
        i++
    }

    for (i in 0 until 23 )  {

        if (i < 16) {
            voltageCompleteArray[i] = voltageFirstEight[i]

            voltageListLeft.add(((voltageFirstEight[4].toInt()*100 + voltageFirstEight[5].toInt()).toFloat()/1000).toString())

            voltageListLeft.add(((voltageFirstEight[6].toInt()*100 + voltageFirstEight[7].toInt()).toFloat()/1000).toString())

            voltageList.add(((voltageFirstEight[0].toInt()*100 + voltageFirstEight[1].toInt()).toFloat()/1000).toString())
            voltageList.add(((voltageFirstEight[2].toInt()*100 + voltageFirstEight[3].toInt()).toFloat()/1000).toString())
            voltageList.add(((voltageFirstEight[4].toInt()*100 + voltageFirstEight[5].toInt()).toFloat()/1000).toString())
            voltageList.add(((voltageFirstEight[6].toInt()*100 + voltageFirstEight[7].toInt()).toFloat()/1000).toString())

        }else {
            voltageCompleteArray[i] = voltageSecondFour[i - 16]

            voltageList.add(((voltageSecondFour[0].toInt()*100 + voltageSecondFour[1].toInt()).toFloat()/1000).toString())
            voltageList.add(((voltageSecondFour[2].toInt()*100 + voltageSecondFour[3].toInt()).toFloat()/1000).toString())
            voltageList.add(((voltageSecondFour[4].toInt()*100 + voltageSecondFour[5].toInt()).toFloat()/1000).toString())
            voltageList.add(((voltageSecondFour[6].toInt()*100 + voltageSecondFour[7].toInt()).toFloat()/1000).toString())

        }

    }

    // try to touch View of UI thread
    activity!!.runOnUiThread(java.lang.Runnable {

        adapterVoltage.notifyDataSetChanged()

        adapterVoltageLeft.notifyDataSetChanged()
    })
}).start()             */




//The following function is for early version of scanning BLE devices

/*
private fun scanDevice(enable: Boolean) {


    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    bluetoothLeScanner = m_bluetoothAdapter!!.getBluetoothLeScanner()

    val m_Handler = Handler()
    if (enable) {

        //     val scanFilters: ArrayList<ScanFilter> = ArrayList()
        // val settings = ScanSettings.Builder().build()
        //    val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("1234")).build()
        //    scanFilters.add(scanFilter)
        //       m_Scanning = true
        //       bluetoothLeScanner!!.startScan(scanCallback)
        // bluetoothLeScanner!!.startScan(scanFilters, settings, scanCallback)


        m_Handler.postDelayed({

            m_Scanning = false
            //       progressBar!!.setVisibility(View.INVISIBLE)
            bluetoothLeScanner!!.stopScan(scanCallback)

        }, 3000)

        m_Scanning = true
        bluetoothLeScanner!!.startScan(scanCallback)


    } else {
        m_Scanning = false

        bluetoothLeScanner!!.stopScan(scanCallback)

    }
}             */