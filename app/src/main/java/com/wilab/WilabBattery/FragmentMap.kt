package com.wilab.WilabBattery


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentMap : Fragment(), OnMapReadyCallback{

    private lateinit var mMap: GoogleMap
    private lateinit var mMapView: MapView
    lateinit var rootView: View

    companion object {
        var mapFragment : SupportMapFragment?=null
        val TAG: String = FragmentMap::class.java.simpleName
        fun newInstance() = FragmentMap()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        println("ok, first")

         rootView = inflater.inflate(R.layout.fragment_fragment_map, container, false)

        //       mapFragment = childFragmentManager.findFragmentById(R.id.fallasMap) as SupportMapFragment?
        //      mapFragment?.getMapAsync(this)
        //  handling map in Fragment is a failure. The code is kept as a reminder.

        return rootView

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMapView = rootView.findViewById(R.id.fallasMap)
        if (mMapView != null){

            mMapView.onCreate(null)
            mMapView.onResume()
            mMapView.getMapAsync(this)

        }



    }
    override fun onMapReady(googleMap: GoogleMap) {

        MapsInitializer.initialize(context)

        mMap = googleMap

     //   googleMap.setMapStyle(GoogleMap!.MAP_TYPE_NORMAL)

        // Add a marker in Sydney and move the camera
        val myPlace = LatLng(40.73, -73.99)  // this is New York

        mMap.addMarker(MarkerOptions().position(myPlace).title("My Favorite City"))

        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPlace))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 12.0f))
    }


}


