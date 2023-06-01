package com.techno.monocle.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Instrumentation
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.Service.START_NOT_STICKY
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.techno.monocle.R
import com.techno.monocle.data.db.remote.FirebaseDataSource
import com.techno.monocle.util.forceHideKeyboard
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.techno.monocle.service.BluetoothBackgroundService


class MainActivity : AppCompatActivity() {

    private lateinit var navView: BottomNavigationView
    private lateinit var mainProgressBar: ProgressBar
    private lateinit var mainToolbar: Toolbar
    private lateinit var notificationsBadge: BadgeDrawable
    private val viewModel: MainViewModel by viewModels()
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val LOCATION_PERMISSION_REQUEST = 123
    private var listView: ListView? = null
    private val BLUETOOTH_REQUEST_CODE = 2
    private val bluetoothDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val mDeviceList = ArrayList<String>()
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 1
    }

//    var storge_permissions = arrayOf(
//        Manifest.permission.CAMERA,
//        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//        Manifest.permission.READ_EXTERNAL_STORAGE
//    )
//
//    @RequiresApi(api = Build.VERSION_CODES.T)
//    var storge_permissions_33 = arrayOf(
//        Manifest.permission.CAMERA,
//        Manifest.permission.READ_MEDIA_IMAGES,
//    )

//    fun permissions(): Array<String>? {
//        val p: Array<String>
//        p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            storge_permissions_33
//        } else {
//            storge_permissions
//        }
//        return p
//    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //   showNotification()
        // Request Bluetooth permissions
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN
//            ),
//            REQUEST_BLUETOOTH_PERMISSION
//        )

//        val permissions = permissions()
//
//        if (permissions != null) {
//            ActivityCompat.requestPermissions(
//                this,
//                permissions,
//                1)
//        };
        mainToolbar = findViewById(R.id.main_toolbar)
        navView = findViewById(R.id.nav_view)
        mainProgressBar = findViewById(R.id.main_progressBar)

        notificationsBadge =
            navView.getOrCreateBadge(R.id.navigation_notifications).apply { isVisible = false }

        setSupportActionBar(mainToolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {
                R.id.profileFragment -> navView.visibility = View.GONE
                R.id.chatFragment -> navView.visibility = View.GONE
                R.id.startFragment -> navView.visibility = View.GONE
                R.id.loginFragment -> navView.visibility = View.GONE
                R.id.createAccountFragment -> navView.visibility = View.GONE
                else -> navView.visibility = View.VISIBLE
            }
            showGlobalProgressBar(false)
            currentFocus?.rootView?.forceHideKeyboard()
        }

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_chats,
                R.id.navigation_notifications,
                R.id.navigation_users,
                R.id.navigation_settings,
                R.id.startFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        bluetoothManager=getSystemService(BluetoothManager::class.java)
        bluetoothAdapter=bluetoothManager.adapter


        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted
            // Start using Bluetooth features
            startBluetoothOperation()
        } else {
            // Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
        /// Start Background Service
        val serviceIntent = Intent(this, BluetoothBackgroundService::class.java)
        this.startService(serviceIntent)

    }
    private fun startBluetoothOperation() {
        // Start using Bluetooth features here
        // Check if Bluetooth permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request Bluetooth permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                BLUETOOTH_REQUEST_CODE
            )
        } else {
            // Permission is already granted, enable Bluetooth
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            // Handle the error here
        } else {
            // Enable Bluetooth
            if (!bluetoothAdapter.isEnabled) {
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                startActivityForResult(enableBluetoothIntent, BLUETOOTH_REQUEST_CODE)
            } else {
                // Bluetooth is already enabled
                // Continue with your logic here

                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                pairedDevices?.forEach { device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    Log.d("TAG", "enableBluetooth: "+deviceName)
                    Log.d("TAG23", "enableBluetooth: "+deviceHardwareAddress)

                    mDeviceList.add(device.getName() + "\n" + device.getAddress())
                    Log.i("BT", device.getName() + "\n" + device.getAddress())
//                    listView!!.adapter = ArrayAdapter<String>(
//                        baseContext,
//                        android.R.layout.simple_list_item_1, mDeviceList
//                    )
                }
                Log.d("TAG", "enableBluetooth: ")

            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled successfully
                // Continue with your logic here
            } else {
                // Bluetooth enabling was canceled or failed
                // Handle the error here
            }
        }
    }
    override fun onPause() {
        super.onPause()
        FirebaseDataSource.dbInstance.goOffline()
    }

    override fun onResume() {
        FirebaseDataSource.dbInstance.goOnline()
        setupViewModelObservers()
        super.onResume()
    }

    private fun setupViewModelObservers() {
        viewModel.userNotificationsList.observe(this, {
            if (it.size > 0) {
                notificationsBadge.number = it.size
                notificationsBadge.isVisible = true
            } else {
                notificationsBadge.isVisible = false
            }
        })
    }

    fun showGlobalProgressBar(show: Boolean) {
        if (show) mainProgressBar.visibility = View.VISIBLE
        else mainProgressBar.visibility = View.GONE
    }




    private fun getPendingIntent(): PendingIntent? {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission granted, start Bluetooth operations
                startBluetoothOperations()
            } else {
                // Bluetooth permission denied, handle it accordingly
                Toast.makeText(this, "Bluetooth permission denied. Cannot proceed with Bluetooth operations.", Toast.LENGTH_SHORT).show()
                stopService(Intent(this, BluetoothPermissionService::class.java))
            }
            finish()
        }
    }
    private fun startBluetoothOperations() {
        // Bluetooth permission granted, start your Bluetooth operations here
        // For example, you can start your Bluetooth connection, send/receive data, etc.

        // Stop the service after Bluetooth operations are started
        stopService(Intent(this, BluetoothPermissionService::class.java))
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        val serviceIntent = Intent(this, BluetoothBackgroundService::class.java)
        this.stopService(serviceIntent)

    }

}



class BluetoothPermissionService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check Bluetooth permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Request Bluetooth permission
            val permissionIntent = Intent(this, MainActivity::class.java)
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(permissionIntent)
        } else {
            // Bluetooth permission already granted, start your Bluetooth operations here
            startBluetoothOperations()
        }

        return START_NOT_STICKY
    }

    private fun startBluetoothOperations() {
        // TODO: Start your Bluetooth operations here
    }
}