package com.wilab.WilabBattery

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import java.io.IOException
import java.util.*

class ControlActivity : AppCompatActivity() {

    //companion object is declared to facilitate data transfer between activities
    //still unclear why the data is defined in companion object.

    companion object{
        var m_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressBar
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        var m_address: String? = null
        var m_mcuID: String = ""
        var m_mcuID_QR: String = ""

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
    }

    private fun sendCommand(input: String){
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun disconnect() {
        if (m_bluetoothSocket == null){

            try {

                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            }catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String> () {
        private var connectSuccess: Boolean = true
        private var context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressBar(context)
            m_progress.setVisibility(View.VISIBLE)
        }


        override fun doInBackground(vararg p0: Void?): String?{
            try {
                if (m_bluetoothSocket == null || !m_isConnected){

                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)

                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord((m_UUID))
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()

                }
            }catch (e: IOException){
                connectSuccess = false
                e.printStackTrace()
            }

            return null
        }

         fun onPostExecution(result: String?){
            super.onPreExecute()
            if (!connectSuccess){
                Log.i("data", "could not connect")
            }else {
                m_isConnected = true
            }
            m_progress.setVisibility(View.GONE)
        }
    }




}
