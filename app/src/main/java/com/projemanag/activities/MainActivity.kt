package com.projemanag.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.projemanag.R

class MainActivity : AppCompatActivity() {

    /**
     * This function is auto created by Android when the Activity Class is created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        //This call the parent constructor
        super.onCreate(savedInstanceState)

        // This is used to align the xml view to this class
        setContentView(R.layout.activity_main)
    }
}
