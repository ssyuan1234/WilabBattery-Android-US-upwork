package com.wilab.WilabBattery


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.synthetic.main.fragment_fragment_product.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentProduct : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val productview = inflater.inflate(R.layout.fragment_fragment_product, container, false)

        val ebikeconfig = productview.findViewById<Button>(R.id.ebikeconfig)
        val toTestPage = productview.findViewById<Button>(R.id.toTestPage)

        ebikeconfig.setOnClickListener {
            val intent = Intent(activity, ebikeConfigActivity::class.java)
            startActivity(intent)
        }



        toTestPage.setOnClickListener {


            val intent = Intent(activity, TestPageActivity::class.java)
            startActivity(intent)


        }


        return productview
    }


}
