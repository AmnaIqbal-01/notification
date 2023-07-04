package com.notification

import android.Manifest
import com.bumptech.glide.Glide

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.storage.FirebaseStorage
import com.notification.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val channelId = "my_channel_id"
    private val notificationId = 1
    private val permissionId = 2
     private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        storage = FirebaseStorage.getInstance()

        binding.btnNotification.setOnClickListener {
            showNotification()
            getLocation()

        }

        binding.btnPic.setOnClickListener{
            openCamera()
        }

    }

    private fun openCamera() {
        if (checkPermissionsCamera()) {
            if (isCameraPermissionEnabled()) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                } catch (e: ActivityNotFoundException) {
                    // display error state to the user
                }}
        }
        else{
            requestCameraPermission()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            //binding.image.setImageBitmap(imageBitmap)
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child("images").child(fileName)
            // Convert the Bitmap to a byte array
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // Upload the image to Firebase Storage
            val uploadTask = storageRef.putBytes(data)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        Log.d("MainActivity", "Download URL: $downloadUrl")

                        // Use the downloadUrl with Glide to load and display the image
                        Glide.with(this)
                            .load(downloadUrl)
                            .error(R.drawable.img) // Set an error image if loading fails
                            .into(binding.image)

                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT)
                            .show()
                        // You can save the downloadUrl or use it to display the image later
                    } } else {
                    // Image upload failed
                    val exception = task.exception
                    // Handle the exception
                }
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            permissionId
        )
    }
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    private fun checkPermissionsCamera(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    private fun getLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
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
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        binding.tvLatitude.text = location.latitude.toString()
                        binding.tvLongitude.text = location.longitude.toString()

                    }
                }
            }
            else {
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }


            }
        private fun isLocationEnabled(): Boolean {
            val locationManager: LocationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }
    private fun isCameraPermissionEnabled(): Boolean {
        val permission = Manifest.permission.CAMERA
        val result = ContextCompat.checkSelfPermission(this, permission)
        return result == PackageManager.PERMISSION_GRANTED
    }
        private fun showNotification() {
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Notification")
                .setContentText("notification example")
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = NotificationManagerCompat.from(this)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
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
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }


