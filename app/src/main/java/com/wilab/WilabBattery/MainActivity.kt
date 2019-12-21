package com.wilab.WilabBattery


import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.*
import android.R.attr.key
import android.content.Intent
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import android.util.SparseArray
import android.view.MenuItem
import com.wilab.WilabBattery.ControlActivity.Companion.m_bluetoothAdapter


class MainActivity : AppCompatActivity() {



    val manager = supportFragmentManager

    private var m_bluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_LOCATION_PERMISSION = 2018


    val fragment11: Fragment = FragmentSystem()
    val fragment22: Fragment = FragmentNetwork()
    val fragment33: Fragment = FragmentProduct()
    val fragment44: Fragment = FragmentWebview()
    val fm = supportFragmentManager
    var active = fragment11




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

    //    navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)


        //bluetooth initiation is moved to MainActivity
    //    bluetoothInit()                 //checklLocationPermission function has called for bluetoothInit()
        checkLocationPermission()


        //this approach will start all four fragments, but hide three of them.

        fm.beginTransaction().add(R.id.fragmentholder, fragment44, "4").hide(fragment44).commit();
        fm.beginTransaction().add(R.id.fragmentholder, fragment33, "3").hide(fragment33).commit();
        fm.beginTransaction().add(R.id.fragmentholder, fragment11, "1").commit();
        fm.beginTransaction().add(R.id.fragmentholder,fragment22, "2").hide(fragment22).commit();


        navView.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_system -> {
                    fm.beginTransaction().hide(active).show(fragment11).commit()
                    active = fragment11
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_network -> {
                    fm.beginTransaction().hide(active).show(fragment22).commit()
                    active = fragment22
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_product -> {
                    fm.beginTransaction().hide(active).show(fragment33).commit()
                    active = fragment33
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_contact -> {
                    fm.beginTransaction().hide(active).show(fragment44).commit()
                    active = fragment44
                    return@OnNavigationItemSelectedListener true
                }

            }
            false
        })


        //legacy code to play music
        //val mp = MediaPlayer.create (this, R.raw.trumpet)
        //      mp.start ()
        //  createFragmentNetwork()
    }




    private fun checkLocationPermission() {
        if (isAboveMarshmallow()) {
            when {
                isLocationPermissionEnabled() -> bluetoothInit()
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) -> displayRationale()
                else -> requestLocationPermission()
            }
        } else {
            bluetoothInit()
        }
    }


    //version screener?
    private fun isAboveMarshmallow(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }


    //Google出于保护用户数据安全的目的，在Android6.0之后，所有需要访问硬件唯一标识符的方面都要申请位置权限（动态申请）
    //    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
    //error message: activity vs. fragment activity. 把这个设置移到了MainActivity，



    private fun isLocationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }


    private fun displayRationale() {
        AlertDialog.Builder(this)
            //         .setMessage(getString(R.string.location_permission_disabled))
            .setMessage("使用蓝牙需位置授权")

            //        .setPositiveButton(getString(R.string.ok)
            .setPositiveButton("OK")

            { _, _ -> requestLocationPermission() }

            .setNegativeButton("取消")
            { _, _ -> }
            .show()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION_PERMISSION)
    }



    private fun bluetoothInit() {

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        //检测是否有蓝牙模块
        if (m_bluetoothAdapter == null) {

            // Device doesn't support Bluetooth
            Log.d("adapter", "This device does not support bluetooth")

        } else {
            Log.d("adapter", m_bluetoothAdapter.toString())
            Log.d("adapter", "ok")
            println("ok. This device supports bluetooth.")

            m_bluetoothAdapter!!.enable()
            //added later because bluetoothLeScanner returns null

        }

    }

}







//this is legacy code in that the replace() is used in switch tabs
/*   private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
       when (item.itemId) {
           R.id.navigation_system -> {

               createFragmentSystem()

               return@OnNavigationItemSelectedListener true
           }
           R.id.navigation_network -> {

               createFragmentNetwork()

               return@OnNavigationItemSelectedListener true
           }
           R.id.navigation_product -> {


               createFragmentProduct()

               return@OnNavigationItemSelectedListener true
           }

           R.id.navigation_contact -> {


               createFragmentWebview()

               return@OnNavigationItemSelectedListener true
           }
       }

       false
   }  */



/*

    fun createFragmentProduct() {

        val transaction = manager.beginTransaction()
        val fragment = FragmentProduct()

        transaction.replace(R.id.fragmentholder, fragment)
        transaction.addToBackStack(null)
        transaction.commit()


    }

    fun createFragmentSystem() {

        val transaction = manager.beginTransaction()
        val fragment = FragmentSystem()

        transaction.replace(R.id.fragmentholder, fragment)
        transaction.addToBackStack(null)
        transaction.commit()

    }

    fun createFragmentNetwork() {


        val transaction = manager.beginTransaction()
        val fragment = FragmentNetwork()






        transaction.replace(R.id.fragmentholder, fragment)
        transaction.addToBackStack(null)
        transaction.commit()


        /*    val clubs = listOf("Arsenel", "Chelsea", "Dublin", "San Ramon")

        lv_clubs.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, clubs)
        lv_clubs.setOnItemClickListener { parent, view, position, id ->
            Toast.makeText(this, clubs[position], Toast.LENGTH_SHORT).show()

        }    */

    }

    fun createFragmentMap() {

        val transaction = manager.beginTransaction()

        val fragment = FragmentMap()

        transaction.replace(R.id.fragmentholder, fragment)
        transaction.addToBackStack(null)
        transaction.commit()

    }

    fun createFragmentWebview() {

        val transaction = manager.beginTransaction()
        val fragment = FragmentWebview()

        transaction.replace(R.id.fragmentholder, fragment)
        transaction.addToBackStack(null)
        transaction.commit()

    }


*/
