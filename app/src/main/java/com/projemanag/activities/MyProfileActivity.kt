package com.projemanag.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_my_profile.*
import com.projemanag.R
import com.projemanag.firebase.FireStoreHandler
import com.projemanag.models.User
import com.projemanag.utils.Constants
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri : Uri? = null
    private var mProfileImageURL : String = ""
    private lateinit var mUserDetails : User
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)
//        setFonts()
        setActionBar()

        my_profile_user_image.setOnClickListener {
            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Constants.showImageChooser(this)
            }else{
                ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                Constants.READ_STORAGE_PERMISSION_CODE)
            }
        }
        FireStoreHandler().loadUserData(this)

        my_profile_update_btn.setOnClickListener {
            if(mSelectedImageFileUri != null){
                uploadUserImage()
            }else{
                showProgressDialog("")
                updateUserProfileData()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.READ_STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
               Constants.showImageChooser(this)
            }
        }else{
            Toast.makeText(this, "You just denied permission for storage." +
                    "You can allow it from the settings.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //check if the user select a photo from gallery
        if(resultCode == Activity.RESULT_OK
            && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
            && data!!.data != null){
            mSelectedImageFileUri = data.data
            // set the new photo to profile image
            try{
                Glide.with(this)
                    .load(mSelectedImageFileUri)
                    .fitCenter()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(my_profile_user_image)
            }catch (e : IOException){
                Log.e("error", "Not able to get the image")
                e.printStackTrace()
            }
        }
    }

    private fun setFonts(){
        val regularFont: Typeface = Typeface.createFromAsset(assets, "Raleway-Regular.ttf")
        my_profile_name_et.typeface = regularFont
        my_profile_email_et.typeface = regularFont
        my_profile_mobile_et.typeface = regularFont
        my_profile_update_btn.typeface = regularFont
    }

    private fun setActionBar(){
        setSupportActionBar(toolbar_my_profile_activity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white)
            actionBar.title = "My Profile"
        }
        toolbar_my_profile_activity.setNavigationOnClickListener{onBackPressed()}
    }

    fun setUserDataInUI(user : User){
        mUserDetails = user
        //set user image
        Glide.with(this)
            .load(user.image)
            .fitCenter()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(my_profile_user_image)
        //set user details
        my_profile_name_et.setText(user.name)
        my_profile_email_et.setText(user.email)
        if(user.mobile != 0L){
            my_profile_mobile_et.setText(user.mobile.toString())
        }
    }

    // store user image to firebase storage
    private fun uploadUserImage(){
        showProgressDialog("")

        if(mSelectedImageFileUri != null){
            val sRef : StorageReference = FirebaseStorage.getInstance()
                .reference
                .child("USER_IMAGE" + System.currentTimeMillis()
                        + "." + Constants.getFileExtension(this,mSelectedImageFileUri))

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
                Log.e("Firebase Image URL", taskSnapshot.metadata!!.reference!!.downloadUrl!!.toString())

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                    Log.e("Downloadable Image URL", uri.toString())
                    mProfileImageURL = uri.toString()

                    updateUserProfileData()               }
            }.addOnFailureListener{
                exception ->
                Toast.makeText(this,exception.message,Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
        }
    }

    private fun updateUserProfileData(){
        val userHashMap = HashMap<String, Any>()
        var anyChangesMade : Boolean = false

        Log.i("image", mProfileImageURL)

        if(mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image){
            userHashMap[Constants.IMAGE] = mProfileImageURL
            anyChangesMade = true
        }

        if(my_profile_name_et.text.toString() != mUserDetails.name){
            userHashMap[Constants.NAME] = my_profile_name_et.text.toString()
            anyChangesMade = true
        }

        if(my_profile_mobile_et.text.toString() != mUserDetails.mobile.toString()){
            userHashMap[Constants.MOBILE] = my_profile_mobile_et.text.toString().toLong()
            anyChangesMade = true
        }

        if(anyChangesMade)
            FireStoreHandler().updateUserProfileData(this,userHashMap)
        else{
            Toast.makeText(this,"Nothing as been updated!",Toast.LENGTH_SHORT).show()
            hideProgressDialog()
        }
    }

    fun profileUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }


}