package com.compasso.drawroutes.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.compasso.drawroutes.R


class HomeFragment : Fragment() {
    lateinit var listener: HomeFragmentListener

    interface HomeFragmentListener {
        fun goToMap()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.findViewById<ImageView>(R.id.buttonHome).setOnClickListener {
            listener.goToMap()
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is HomeFragmentListener) {
            context
        } else {
            throw RuntimeException(
                "$context must implement FragmentAListener"
            )
        }
    }

}