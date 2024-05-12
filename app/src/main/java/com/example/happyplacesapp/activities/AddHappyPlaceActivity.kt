package com.example.happyplacesapp.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.happyplacesapp.R
import com.example.happyplacesapp.models.HappyPlacesModel
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.utils.GetAddressFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.Permissions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private val cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
    private var mHappyPlaceDetails: HappyPlacesModel? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_happy_place)
        setSupportActionBar(findViewById(R.id.toolbar_add_place))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Happy Place"
        findViewById<Toolbar>(R.id.toolbar_add_place).setNavigationOnClickListener{
            onBackPressed()
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getSerializableExtra(
                MainActivity.EXTRA_PLACE_DETAILS) as HappyPlacesModel
        }
        dateSetListener = DatePickerDialog.OnDateSetListener{
            _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        updateDateInView()
        if(mHappyPlaceDetails != null){
            supportActionBar?.title = "Edit Happy Place Details"
            findViewById<TextView>(R.id.et_title).text = mHappyPlaceDetails!!.title
            findViewById<TextView>(R.id.et_description).text = mHappyPlaceDetails!!.description
            findViewById<TextView>(R.id.et_date).text = mHappyPlaceDetails!!.date
            findViewById<TextView>(R.id.et_location).text = mHappyPlaceDetails!!.location
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude
            saveImageToInternalStorage = Uri.parse(
                mHappyPlaceDetails!!.image)
            findViewById<ImageView>(R.id.iv_place_image).setImageURI(saveImageToInternalStorage)
            findViewById<Button>(R.id.btn_save).text = "UPDATE"
        }
        findViewById<TextView>(R.id.et_date).setOnClickListener(this)
        findViewById<TextView>(R.id.tv_add_image).setOnClickListener(this)
        findViewById<Button>(R.id.btn_save).setOnClickListener(this)
        findViewById<TextView>(R.id.tv_select_current_location).setOnClickListener(this)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
           val mLastLocation: Location? = locationResult.lastLocation
            mLatitude = mLastLocation!!.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")
            val addressTask = GetAddressFromLatLng(
                this@AddHappyPlaceActivity,
                mLatitude,
                mLongitude
                )
            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                   findViewById<TextView>(R.id.et_location).text=address
                }
                override fun onError() {
                    Log.e("Get Address :: ", "Something went wrong")
                }
            })
            addressTask.getAddress()
        }
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.et_date -> {
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf(
                    "Select photo from Gallery",
                    "Capture photo from camera"
                )
                pictureDialog.setItems(pictureDialogItems){
                    _, which ->
                        when(which){
                            0 -> choosePhotoFromGallery()
                            1 -> takePhotoFromCamera()
                        }
                    }
                    pictureDialog.show()
                }
            R.id.btn_save -> {
                when{
                    findViewById<TextView>(R.id.et_title).text.isNullOrEmpty() -> {
                        Toast.makeText(this@AddHappyPlaceActivity,"Please enter title",
                            Toast.LENGTH_SHORT).show()
                    }
                    findViewById<TextView>(R.id.et_description).text.isNullOrEmpty() -> {
                        Toast.makeText(this@AddHappyPlaceActivity,"Please enter description",
                            Toast.LENGTH_SHORT).show()
                    }
                    findViewById<TextView>(R.id.et_location).text.isNullOrEmpty() -> {
                        Toast.makeText(this@AddHappyPlaceActivity,"Please enter location",
                            Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this@AddHappyPlaceActivity,"Please select an image",
                            Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val happyPlacesModel = HappyPlacesModel(
                           if(mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            findViewById<TextView>(R.id.et_title).text.toString(),
                            saveImageToInternalStorage.toString(),
                            findViewById<TextView>(R.id.et_description).text.toString(),
                            findViewById<TextView>(R.id.et_date).text.toString(),
                            findViewById<TextView>(R.id.et_location).text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)
                        if(mHappyPlaceDetails == null){
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlacesModel)
                            if(addHappyPlace>0){
                                setResult(Activity.RESULT_OK)
                                Toast.makeText(this@AddHappyPlaceActivity,
                                    "The happy place details are inserted successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish();
                            }
                        }
                        else{
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlacesModel)
                            if(updateHappyPlace>0){
                                setResult(Activity.RESULT_OK)
                                Toast.makeText(this@AddHappyPlaceActivity,
                                    "The happy place details are updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish();
                            }
                        }
                    }
                }
              }
            R.id.tv_select_current_location -> {
                  if(!isLocationEnabled()){
                      Toast.makeText(this, "Your location provider is turned off. Please turn it on.",
                          Toast.LENGTH_SHORT).show()

                      val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                      startActivity(intent)
                  }
                  else{
                      Dexter.withActivity(this).withPermissions(
                          android.Manifest.permission.ACCESS_FINE_LOCATION,
                          android.Manifest.permission.ACCESS_COARSE_LOCATION)
                          .withListener(object : MultiplePermissionsListener {
                              override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                  if(report!!.areAllPermissionsGranted()){
                                     requestNewLocationData()
                                  }
                              }

                              override fun onPermissionRationaleShouldBeShown(
                                  permissions: MutableList<PermissionRequest>?,
                                  token: PermissionToken?
                              ) {
                                  showRationalDialogForPermissions()
                              }
                          }).onSameThread()
                          .check()
                  }
              }
            }
        }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY){
                if(data!=null){
                    val contentURI = data.data
                    try{
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                        saveImageToInternalStorage =  saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("saved image", "Path :: $saveImageToInternalStorage")
                        Toast.makeText(this@AddHappyPlaceActivity,
                            "Image saved successfully", Toast.LENGTH_SHORT).show()
                        findViewById<ImageView>(R.id.iv_place_image).setImageBitmap(selectedImageBitmap)

                    }catch (e: IOException){
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity,
                            "Failed to load the image from gallery", Toast.LENGTH_SHORT).show()
                    }
                }
            }else if(requestCode == CAMERA){
                val thumbNail : Bitmap = data!!.extras!!.get("data") as Bitmap
                 saveImageToInternalStorage =  saveImageToInternalStorage(thumbNail)
                Log.e("saved image", "Path :: $saveImageToInternalStorage")
                findViewById<ImageView>(R.id.iv_place_image).setImageBitmap(thumbNail)
            }
        }
    }

    private fun takePhotoFromCamera(){
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(galleryIntent, CAMERA)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationalDialogForPermissions()
            }

        }).onSameThread().check()
    }

    private fun choosePhotoFromGallery(){
       Dexter.withActivity(this).withPermissions(
           android.Manifest.permission.READ_EXTERNAL_STORAGE,
           android.Manifest.permission.WRITE_EXTERNAL_STORAGE
       ).withListener(object : MultiplePermissionsListener {
           override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
               if(report!!.areAllPermissionsGranted()){
                  val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                   startActivityForResult(galleryIntent, GALLERY)
               }
           }

           override fun onPermissionRationaleShouldBeShown(
               permissions: MutableList<PermissionRequest>?,
               token: PermissionToken?
           ) {
                showRationalDialogForPermissions()
           }

       }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage(
            "It looks like you have turned off permission required for this feature."
            + "It can be enabled under the Applications Settings"
        )
            .setPositiveButton("GO TO SETTINGS")
            { _,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton(
                "CANCEL"
            ){
                dialog, _ -> dialog.dismiss()
            }.show()
    }

    private fun updateDateInView(){
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        findViewById<TextView>(R.id.et_date).setText(sdf.format(cal.time)).toString()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.png")
        try{
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    companion object{
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
    }
}