package com.projemanag.activities

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.projemanag.R
import com.projemanag.adapters.BoardItemAdapter
import com.projemanag.firebase.FireStoreHandler
import com.projemanag.models.Board
import com.projemanag.models.User
import com.projemanag.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.main_content.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    /**
     * This function is auto created by Android when the Activity Class is created.
     */
    companion object{
        const val MY_PROFILE_REQUEST_CODE : Int = 11
        const val CREATE_BOARD_REQUEST_CODE : Int = 21
    }

    private lateinit var mUserName : String
    private lateinit var mSharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        //This call the parent constructor
        super.onCreate(savedInstanceState)

        // This is used to align the xml view to this class
        setContentView(R.layout.activity_main)

        navigator_view.setNavigationItemSelectedListener(this)

        FireStoreHandler().loadUserData(this, true)

        create_board_fab.setOnClickListener {
            val intent = Intent(this,CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }

        setActionBar()
    }

    fun populateBoardsToUI(boardsList : ArrayList<Board>){
        hideProgressDialog()

        if(boardsList.size > 0 ){
            boards_list_rv.visibility = View.VISIBLE
            no_boards_tv.visibility = View.GONE

            boards_list_rv.layoutManager = LinearLayoutManager(this)
            boards_list_rv.setHasFixedSize(true)

            val adapter = BoardItemAdapter(this, boardsList)
            boards_list_rv.adapter = adapter

            adapter.setOnClickListener(object : BoardItemAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentID)
                    startActivity(intent)
                }
            })
        }else{
            boards_list_rv.visibility = View.GONE
            no_boards_tv.visibility = View.VISIBLE
        }
    }

    private fun setActionBar(){
        setSupportActionBar(toolbar_main_activity)
        toolbar_main_activity.setNavigationIcon(R.drawable.ic_navigation_manu)
        toolbar_main_activity.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    override fun onBackPressed() {
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            doubleBackToExit()
        }
    }

    private fun toggleDrawer(){
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }
    // set logged user image and name to the navigator
    fun updateNavigationUserDetails(user : User, readBoardsList : Boolean){
//        hideProgressDialog()
        mUserName = user.name

        // add image
        if(user.image.isEmpty()){
            showErrorSnackBar("Image not loading")
        }
        else{
            Glide.with(this)
                .load(user.image)
                .fitCenter()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(nav_user_image)
        }

        // add name
        user_name_tv.text = user.name

        if(readBoardsList){
            showProgressDialog("")
            FireStoreHandler().getBoardsList(this)
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile -> {
                Log.i("info", "profilenav")
                startActivityForResult(Intent(this, MyProfileActivity::class.java), MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply() //reset the shared prefs - make sure that shared preferences not stored
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE){
            FireStoreHandler().loadUserData(this)
        }else if(resultCode == Activity.RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE){
            FireStoreHandler().getBoardsList(this)
        } else{
            Log.e("MainOnActivityResult", "Cancelled")
        }
    }

    fun tokenUpdateSuccess(){
        hideProgressDialog()
        val editor : SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog("")
        FireStoreHandler().loadUserData(this,true)
    }

    private fun updateFcmToken(token : String){
        val userHashMap = HashMap<String,Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showProgressDialog("")
        FireStoreHandler().updateUserProfileData(this,userHashMap)
    }
}
