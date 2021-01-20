package com.example.videocallapp

import android.webkit.JavascriptInterface

//take to CallActivity!
class JavascriptInterface(val callActivity: CallActivity) {

    @JavascriptInterface
    //this func call from the javascript
    public fun onPeerConnected() {
        callActivity.onPeerConnected() //callActivity에서 호출
    }

}