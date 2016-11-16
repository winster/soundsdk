package com.bawali.soundsdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bawali.soundsdk.audio.service.SessionService;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button transmitButton;
    Button receiveButton;
    EditText dataEditText;
    TextView receivedDataTextView;

    private Handler mHandler = new Handler();
    private SessionService mSessionService;
    private Timer refreshTimer = null;



    private ServiceConnection mSessionConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mSessionService = ((SessionService.SessionBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mSessionService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this, SessionService.class), mSessionConnection,Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        unbindService(mSessionConnection);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        transmitButton = (Button) findViewById(R.id.transmitButton);
        receiveButton = (Button) findViewById(R.id.receiveButton);
        dataEditText = (EditText) findViewById(R.id.dataEditText);
        receivedDataTextView = (TextView) findViewById(R.id.receivedDataTextView);
        transmitButton.setOnClickListener(this);
        receiveButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        doBindService();

        refreshTimer = new Timer();

        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() // have to do this on the UI thread
                {
                    public void run() {
                        updateResults();
                    }
                });
            }
        }, 5000, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        doUnbindService();
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        mSessionService.stopListening();
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.transmitButton) {
            mSessionService.playData(dataEditText.getText().toString(),SessionService.SessionStatus.PLAYING, 0);
        } else if(view.getId()==R.id.receiveButton) {
            mSessionService.listen();
        }
        dataEditText.setText("");
    }

    private void updateResults() {
        if (mSessionService.getStatus() == SessionService.SessionStatus.LISTENING) {
            if (!mSessionService.getListenString().isEmpty()) {
                Log.e("SoundSDK", mSessionService.getListenString());
                receivedDataTextView.setText(mSessionService.getListenString());
                mSessionService.stopListening();
            }
        } else if (mSessionService.getStatus() == SessionService.SessionStatus.FINISHED) {
            Log.e("SoundSDK", "FINISHED");
        } else {
            Log.e("SoundSDK", mSessionService.getStatus()+"");
        }
    }
}
