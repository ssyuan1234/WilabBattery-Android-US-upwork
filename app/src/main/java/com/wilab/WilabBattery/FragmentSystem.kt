package com.wilab.WilabBattery


import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_fragment_system.*
import android.media.RingtoneManager
import android.media.Ringtone
import android.R.id.message
import android.app.AlertDialog
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.app.NotificationManager
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import androidx.media.app.NotificationCompat
import android.media.AudioManager
import android.os.*
import androidx.core.content.ContextCompat.getSystemService
import java.nio.file.Files.exists
import android.os.Environment.getExternalStorageDirectory
import java.io.File
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.*

class FragmentSystem : Fragment() {

    companion object{
        fun newInstance(): FragmentSystem {
            return FragmentSystem()
        }

    }

    private var m_bluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private lateinit var mdevice: BluetoothDevice

    //used to differentiate different request after service discovered. If from write request, requestNumber = 1
    var requestNumber = 2

    private var m_characteristic: BluetoothGattCharacteristic? = null
    private var m_write_characteristic: BluetoothGattCharacteristic? = null

    var commandString: String = "connect"
    var preStoredDeviceAddress: String = " "
    lateinit var m_address: String

    val uuid_service_wilab_device = "0000fff0-0000-1000-8000-00805f9b34fb"
    val uuid_characteristic_wilab_device = "0000fff1-0000-1000-8000-00805f9b34fb"
    val uuid_write_characteristic_wilab_device = "0000fff2-0000-1000-8000-00805f9b34fb"


    private lateinit var statusUpdate: TextView
    var voltageList = arrayOfNulls<String>(20)


    private lateinit var listVoltageView: ListView
    private lateinit var adapterVoltage: ArrayAdapter<String?>

    //array for data displayed on the left, 8 voltage data points
    var voltageListLeft = arrayOfNulls<String>(20)
    private lateinit var listVoltageViewLeft: ListView
    private lateinit var adapterVoltageLeft: ArrayAdapter<String?>

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


  //  private var voltageListVM = arrayOf("0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0")

    //The following code is message handler to update data and status.
    /*       handleMessage() defines the operations to perform when
      the Handler receives a new Message to process.                     */


    val handler: Handler = object : Handler(Looper.getMainLooper()) {

        override fun handleMessage(inputMessage: Message) {

            // Gets the image task from the incoming Message object.
            if (inputMessage.what == 1) {

                statusUpdate.blink(3)

                //          val viewModel = ViewModelProvider(this@FragmentNetwork).get(SharedViewModel::class.java)
                //          viewModel.update(voltageListVM)


                batterysoc.text = voltageList[5]

                statusReport.text = resources.getString(R.string.normal)


                if (errorCode[3] == 1) {statusReport.text = resources.getString(R.string.over_charge)}

                //this file path has to be defined in handle message section. Otherwise, program crash
                val filePath = context!!.getFilesDir().path.toString() + "/fileName.txt"

                //this section is used to write data to the file
                if (((SystemClock.elapsedRealtime() - previousTime) > 600000) ||  (fileWrite_Firsttime == 1)) {


                    //Every hour write into data file
                    previousTime = SystemClock.elapsedRealtime()

                    //because voltageList has allocated more space then I intended to have, I do not need all the info in voltageList




                    jointString = voltageList.joinToString (separator = ",") {it -> "$it"}
                    jointStringLeft = voltageListLeft.joinToString (separator = ",") {it -> "$it"}

                    //This is writing time stamp and data into the system

                    File(filePath).appendText("${SystemClock.elapsedRealtime()}, $jointString, $jointStringLeft\n")

                    fileWrite_Firsttime = 0

                }



            }

            if (inputMessage.what == 2) {

                statusUpdate.text = resources.getString(R.string.connection_status_device_found)

            }

            if (inputMessage.what == 3) {

                statusUpdate.text = resources.getString(R.string.connection_status_connection_ongoing)

            }

            if (inputMessage.what == 4) {

                statusUpdate.text = resources.getString(R.string.connection_status_search_again)

            }

            if (inputMessage.what == 5) {

                statusUpdate.text = resources.getString(R.string.connection_status_reading_data)

            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        val rootView = inflater.inflate(R.layout.fragment_fragment_system, container, false)



        //this <Button> line sometimes give strange error message without crashing the program
        val updateinfo = rootView.findViewById<Button>(R.id.updateinfo)
        val deviceConnect = rootView.findViewById<Button>(R.id.deviceConnect)

        statusUpdate = rootView.findViewById<TextView>(R.id.statusupdate)

        val lockup = rootView.findViewById<Button>(R.id.lockup)

        // the following seciont will achieve:
        // 1. check whether there is pre-stored device address, if not, tell user to scan BLE device
        // 2. if there is pre-stored device address, scan whether the device is in the range
        // 3. if the pre-stored device is in the range, connect and show the data.
        // 4. the challenge is that I will not be able to show detailed battery data easily. I need to migrate to network page
        // 5. if there is pre-stored device address, how many times do I have to search the device?
        // 6. from broadcasting find out whether the BMS is locked
        // 7. find the device broadcasting information by device name?

        for (i in 0 until 20) { voltageListLeft[i] = "0.0" }
        for (i in 0 until 20) { voltageList[i] = "0.0" }
        for (i in 0 until 8) { errorCode[i] = 0 }


        val filePath = context!!.getFilesDir().path.toString() + "/deviceAddress.txt"

        //if device address is available, the BLE device is connected automatically
        if (File(filePath).exists()) {
            preStoredDeviceAddress = File(filePath).readText()

            m_address = preStoredDeviceAddress

            m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            mdevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)


           mdevice.connectGatt(requireActivity(), false, mGattCallback)



            //    val device = mBluetoothAdapter.getRemoteDevice(preStoredDeviceAddress);
        //    device.connectGatt(this, false, gattCallback);

        } else {
            displayRationale()
            deviceConnect.text = resources.getString(R.string.no_connection)
        }

        if (preStoredDeviceAddress != "") {

            //read device info, should I copy all the file in FragmentNetwork to connect device?
            readDeviceInfo()


        } else
        {
            //show text to scan the device, this dialog page will show up as soon as app is started.
            //    displayRationale()
        }


       deviceConnect.setOnClickListener {

            requestNumber =1
            commandString = "unlock"

           if (File(filePath).exists()) {
               preStoredDeviceAddress = File(filePath).readText()

               m_address = preStoredDeviceAddress

               m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
               mdevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)


               mdevice.connectGatt(requireActivity(), false, mGattCallback)


           }





        }




        lockup.setOnClickListener {

            //write lockup message to BLE


             requestNumber =1
             commandString = "lock"

            if (File(filePath).exists()) {
                preStoredDeviceAddress = File(filePath).readText()

                m_address = preStoredDeviceAddress

                m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                mdevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)


                mdevice.connectGatt(requireActivity(), false, mGattCallback)




            }







        }


        updateinfo.setOnClickListener{

            //if the warning signal is sent out, the warning trumpet will be played.
    //        if (1 == 2) {
    //            val mp = MediaPlayer.create(activity, R.raw.trumpet)
     //           mp.start ()
    //        }


            //get input from EditTexts and save in variables
            val recipient = "cloud@wilabenergy.cn"
            val subject = "test data"
            val message = "hello"

            //method call for email intent with these inputs as parameters
            sendEmail(recipient, subject, message)

    //        updateSystem(strtext)

            }

        return rootView





    }

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




    //auto-check BLE device status
    // 1. if there is a device stored in the file and in the range, connect and then show data
    //   change text to "解锁"， "锁上"
    // 2. if there is a device stored in the file, not in the range,
    // 3. if there is no device stored in the file



        private fun displayRationale() {
            AlertDialog.Builder(activity)
             //         .setMessage(getString(R.string.location_permission_disabled))
                 .setMessage(resources.getString(R.string.no_ble_connection_warning))

            //        .setPositiveButton(getString(R.string.ok)
                 .setPositiveButton("OK")

                { _, _ ->  }

                 .setNegativeButton(resources.getString(R.string.cancel_button))
                 { _, _ -> }
                 .show()
    }



            private fun readDeviceInfo(){




            }




            private fun updateSystem(batteryinfo: String) {


                batterysoc.text = batteryinfo

    /*      //    unsupported video capability
         //       val mp = MediaPlayer.create(activity, R.raw.trumpet)
         //       mp.start ()   */

                scanPairedDevice()
            }


            //this function is written for future warning function

            private fun warningNotification(){



                //to refer context in fragment, "this" does not work, instead, "activity" is used

        //        val mp = MediaPlayer.create (activity, R.raw.trumpet)
         //       mp.start ()
            }


            private fun scanPairedDevice() {

                BluetoothAdapter.getDefaultAdapter()

            }



    private fun sendEmail(recipient: String, subject: String, message: String) {

        /*ACTION_SEND action to launch an email client installed on your Android device.*/

        val mIntent = Intent(Intent.ACTION_SEND)


        /*To send an email you need to specify mailto: as URI using setData() method
        and data type will be to text/plain using setType() method*/

        mIntent.data = Uri.parse("mailto:")

       mIntent.type = "text/plain"


        // put recipient email in intent
        /* recipient is put as array because you may wanna send email to multiple emails
           so enter comma(,) separated emails, it will be stored in array*/

        mIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))

        //put the Subject in the intent
        mIntent.putExtra(Intent.EXTRA_SUBJECT, subject)

        //put the message in the intent
        mIntent.putExtra(Intent.EXTRA_TEXT, message)



              mIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                   mIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
               }
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                   mIntent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
               }


        val filePath = context!!.getFilesDir().path.toString() + "/fileName.txt"
        val file = File(filePath)

    //    val uri = Uri.parse("file://$file")

 //       val content = File(filePath).readText()

 //       println("file content is $content")


        val contentUri = FileProvider.getUriForFile(this.context!!, "com.wilab.WilabBattery.fileprovider", file)




        //put the attachment in the intent
        mIntent.putExtra(Intent.EXTRA_STREAM, contentUri)

        //   Uri.fromFile(file)




        try {
            //start email intent
            startActivity(Intent.createChooser(mIntent, "Choose Email Client..."))
        }
        catch (e: Exception){
            //if any thing goes wrong for example no email client application or any exception
            //get and show exception message
       //     Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
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


                println(requestNumber)
                if (requestNumber == 2) {


                    m_characteristic = gatt!!.getService(UUID.fromString(uuid_service_wilab_device))
                        .getCharacteristic(UUID.fromString(uuid_characteristic_wilab_device))


                    val mm_properties =
                        gatt!!.getService(UUID.fromString(uuid_service_wilab_device))
                            .getCharacteristic(UUID.fromString(uuid_characteristic_wilab_device))
                            .properties

                    if ((mm_properties * BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        gatt!!.setCharacteristicNotification(
                            gatt!!.getService(UUID.fromString(uuid_service_wilab_device)).getCharacteristic(
                                UUID.fromString(uuid_characteristic_wilab_device)
                            ), true

                        )
                    }

                    gatt!!.readCharacteristic(m_characteristic)

                } else

                {
                    if (requestNumber == 1) {

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


                    }


            }

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


                voltageList[4] = resources.getString(R.string.current_reading) + ((data[3].toInt()*100).toFloat()/100).toString()


                voltageList[5] = ((data[2].toInt()*100).toFloat()/100).toString() + "%"



                handler.obtainMessage(UPDATE_TEXT).apply {

                    sendToTarget() }



            } else {





                //          voltageSecondFour = data

                println("2nd half")
                println(Arrays.toString(data))


                //Array<String> element value assignment
                voltageList[0] = "#31,  " + ((data[0].toInt()*100 + data[1].toInt()).toFloat()/1000 + 0.00001).toString().take(5)
                voltageList[1] = "#32,  " + ((data[2].toInt()*100 + data[3].toInt()).toFloat()/1000 + 0.00001).toString().take(5)

                voltageList[2] = "#33,  " + ((data[4].toInt()*100 + data[5].toInt()).toFloat()/1000 + 0.00001).toString().take(5)
                voltageList[3] = "#34,  " + ((data[6].toInt()*100 + data[7].toInt()).toFloat()/1000 + 0.00001).toString().take(5)


                voltageList[6] = resources.getString(R.string.temperature_first_pack) + (((data[8].toInt()*100)-4000).toFloat()/100).toString()

                voltageList[7] = resources.getString(R.string.temperature_second_pack) + (((data[8].toInt()*100)-4000).toFloat()/100).toString()
                voltageList[8] = resources.getString(R.string.temperature_third_pack) + (((data[8].toInt()*100)-4000).toFloat()/100).toString()

                errorCode[0]  = (data[11].toInt() shr 7)  and 0x01        //convert error byte to int, read the first bit
                errorCode[1]  = (data[11].toInt() shr 6)  and 0x01
                errorCode[2]  = (data[11].toInt() shr 5)  and 0x01
                errorCode[3]  = (data[11].toInt() shr 4)  and 0x01
                errorCode[4]  = (data[11].toInt() shr 3)  and 0x01
                errorCode[5]  = (data[11].toInt() shr 2)  and 0x01
                errorCode[6]  = (data[11].toInt() shr 1)  and 0x01
                errorCode[7]  = (data[11].toInt() shr 0)  and 0x01


                voltageList[9] = resources.getString(R.string.communication_abnormal) + errorCode[0].toString()
                voltageList[10] = resources.getString(R.string.over_current) + errorCode[1].toString()
                voltageList[11] = resources.getString(R.string.over_temp) + errorCode[2].toString()
                voltageList[12] = resources.getString(R.string.over_charge) + errorCode[3].toString()
                voltageList[13] = resources.getString(R.string.over_discharge) + errorCode[4].toString()
                voltageList[14] = resources.getString(R.string.charging) + errorCode[5].toString()
                voltageList[15] = resources.getString(R.string.discharging) + errorCode[6].toString()



                /*            println(errorCode[0])
                            println(errorCode[1])
                            println(errorCode[2])
                            println(errorCode[3])
                            println(errorCode[4])
                            println(errorCode[5])
                            println(errorCode[6])
                            println(errorCode[7])       */









                handler.obtainMessage(UPDATE_TEXT).apply { sendToTarget() }




            }


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
