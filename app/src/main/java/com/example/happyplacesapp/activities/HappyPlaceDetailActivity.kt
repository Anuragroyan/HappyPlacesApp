package com.example.happyplacesapp.activities

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.happyplacesapp.R
import com.example.happyplacesapp.models.HappyPlacesModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_happy_place_detail)
        var happyPlacesDetailModel : HappyPlacesModel? = null
        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            happyPlacesDetailModel =
                intent.getSerializableExtra(
                    MainActivity.EXTRA_PLACE_DETAILS
                ) as HappyPlacesModel
        }

        if(happyPlacesDetailModel != null){
            setSupportActionBar(findViewById(R.id.toolbar_happy_place_detail))
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = happyPlacesDetailModel.title
            findViewById<Toolbar>(R.id.toolbar_happy_place_detail).setNavigationOnClickListener {
                onBackPressed()
            }
            findViewById<ImageView>(R.id.iv_place_image).setImageURI(Uri.parse(happyPlacesDetailModel.image))
            findViewById<TextView>(R.id.tv_Description).text = happyPlacesDetailModel.description
            findViewById<TextView>(R.id.tv_location).text = happyPlacesDetailModel.location
        }
    }
}