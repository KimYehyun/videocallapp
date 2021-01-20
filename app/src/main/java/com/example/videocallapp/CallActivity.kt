package com.example.videocallapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_call.*
import java.util.*

class CallActivity : AppCompatActivity() {

    
    var username = ""
    var friendsUsername = ""

    var isPeerConnected = false

    var firebaseRef = Firebase.database.getReference("users")
    
    // call.xml 에서 오디오, 비디오 아이콘 컨트롤
    var isAudio = true
    var isVideo = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)


        // !!는 username이 null이 아님을 알려줌(null이 전달되었을경우 오류)
        username = intent.getStringExtra("username")!!
        //예현추가 --전화걸기전 데이터 저장

        firebaseRef.child("A").child("test").setValue("success")
        firebaseRef.child("B").child("test").setValue("success")
        firebaseRef.child("C").child("test").setValue("success")

        callData.setOnClickListener {
            val intent = Intent(this, CallData::class.java) //액티비티끼리 서로 호출
            intent.putExtra("username", username)
            startActivity(intent)
        }

        callBtn.setOnClickListener {
            friendsUsername = friendNameEdit.text.toString()
            sendCallRequest()
        }

        //오디오버튼 누르면 음소거
        toggleAudioBtn.setOnClickListener {
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            toggleAudioBtn.setImageResource(if (isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24 )
        }
        //비디오버튼누르면 비디오사용중지
        toggleVideoBtn.setOnClickListener {
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
            toggleVideoBtn.setImageResource(if (isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24 )
        }

        setupWebView() //웹뷰 함수 불러옴
    }

    private fun sendCallRequest() {
        if (!isPeerConnected) { //peerjs서버에 연결이 안되었을 경우 메세지 출력
            Toast.makeText(this, "You're not connected. Check your internet", Toast.LENGTH_LONG).show()
            return
        }

        firebaseRef.child(friendsUsername).child("incoming").setValue(username) //내가 전화를 걸음
        firebaseRef.child(friendsUsername).child("isAvailable").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.value.toString() == "true") {
                    listenForConnId()
                    //전화받느느사람이 수락
                }

            }

        })

    }

    private fun listenForConnId() {
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null)
                    return
                switchToControls()
                callJavascriptFunction("javascript:startCall(\"${snapshot.value}\")")
            }

        })
    }

    private fun setupWebView() {

        // 웹뷰한테 웹페이지의 권한을 요청
        webView.webChromeClient = object: WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) { // for this permission!
                request?.grant(request.resources)
                //?.앞의 변수가 null이 아닐때만 오른쪽 함수가 수행되고 null이면 null을 반환
                // 즉 if(request!=null)request.grant(request.resources)
            }
        }

        webView.settings.javaScriptEnabled = true  // 자바스크립스 액션이 가능하게 한다
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavascriptInterface(this), "Android") //자바스크립트 인터페이스 추가

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"  //html 경로 넣기
        webView.loadUrl(filePath) // url(html) 로드

        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
                //웹페이지로드가 끝나면 피어 초기화
            }
        }
    }

    var uniqueId = ""

    private fun initializePeer() {

        uniqueId = getUniqueID()

        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(username).child("incoming").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)

            }

        })

    }
    //xml 보이기 순서
    private fun onCallRequest(caller: String?) {
        if (caller == null) return
        //전화오면 위에 뜸
        callLayout.visibility = View.VISIBLE
        incomingCallTxt.text = "$caller is calling..."

        acceptBtn.setOnClickListener {
            firebaseRef.child(username).child("connId").setValue(uniqueId)
            firebaseRef.child(username).child("isAvailable").setValue(true)
            callLayout.visibility = View.GONE
            switchToControls()
        }

        rejectBtn.setOnClickListener {
            firebaseRef.child(username).child("incoming").setValue(null)
            callLayout.visibility = View.GONE
        }
    }

    private fun switchToControls() {
        //fun onCallRequest에서 사용
        inputLayout.visibility = View.GONE
        callControlLayout.visibility = View.VISIBLE //비디오 마이크 그림 보임
    }


   private fun getUniqueID(): String {
        return UUID.randomUUID().toString() //ID값 (cmd창에뜲)
   }

    private fun callJavascriptFunction(functionString: String) {
        webView.post { webView.evaluateJavascript(functionString, null) }
    }


    fun onPeerConnected() {
        isPeerConnected = true
        //연결 성공했을 경우 true -> peerjs서버가 성공적이어야함
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        firebaseRef.child(username).setValue(null)
        webView.loadUrl("about:blank")
        super.onDestroy()
    }

}