package com.wilab.WilabBattery


import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.fragment_fragment_webview.*
import javax.security.auth.callback.Callback


// private val WebSettings.LOAD_DEFAULT: Any
//    get() {}


//I had some problems with mapView in Fragment before. This time I had some problems with webView. For both cases,
//it seems that I need to set up a view first and then find the webView because, unlike the button, mapView and
//contentView needs view to be there to be found.

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentWebview : Fragment() {

    lateinit var contentView: View


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        contentView = inflater.inflate(R.layout.fragment_fragment_webview, container, false)
        return contentView

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webview = contentView.findViewById<WebView>(R.id.myWebview)

       // val url = "http://www.wilabenergy.cn/447caed745a1420ca07d8d1b2c7fec2c.html"
         val url = "http://map.google.com"



        val webSettings = myWebview.settings

        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true


        webSettings.setSupportZoom(true)                         //支持缩放
        webSettings.setBuiltInZoomControls(true)                 // 设置出现缩放工具

        webSettings.allowFileAccess = true                       // 设置允许访问文件数据
        webSettings.setDatabaseEnabled(true)                     //启用数据库



        //自适应屏幕
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN)
        webSettings.setLoadWithOverviewMode(true)


        webSettings.setGeolocationEnabled(true);//定位



     /*   webview.webChromeClient = object: WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }

        }              */

            //   webSettings.javaScriptCanOpenWindowsAutomatically = true


     //   webSettings.cacheMode = WebSettings.LOAD_NO_CACHE


        webview.webViewClient = WebViewClient()

        webview.loadUrl(url)

    }

}


