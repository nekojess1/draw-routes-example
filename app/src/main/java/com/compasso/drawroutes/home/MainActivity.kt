package com.compasso.drawroutes.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.compasso.drawroutes.R
import com.compasso.drawroutes.home.HomeFragment
import com.compasso.drawroutes.ui.map.MapsFragment

class MainActivity : AppCompatActivity(), HomeFragment.HomeFragmentListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        changeFragment(HomeFragment())
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_up, R.anim.slide_up)
            .replace(R.id.container_layout, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun goToMap() {
        changeFragment(MapsFragment())
    }


}