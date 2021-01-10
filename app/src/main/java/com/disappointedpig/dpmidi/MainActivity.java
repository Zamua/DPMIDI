package com.disappointedpig.dpmidi;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.disappointedpig.midi.MIDIConstants;
import com.disappointedpig.midi.MIDISession;
import com.disappointedpig.midi.events.MIDIConnectionEndEvent;
import com.disappointedpig.midi.events.MIDIConnectionEstablishedEvent;
import com.disappointedpig.midi.events.MIDIConnectionRequestAcceptedEvent;
import com.disappointedpig.midi.events.MIDIConnectionRequestReceivedEvent;
import com.disappointedpig.midi.events.MIDIConnectionRequestRejectedEvent;
import com.disappointedpig.midi.events.MIDIConnectionSentRequestEvent;
import com.disappointedpig.midi.events.MIDIReceivedEvent;
import com.disappointedpig.midi.events.MIDISessionNameRegisteredEvent;
import com.disappointedpig.midi.events.MIDISessionStartEvent;
import com.disappointedpig.midi.events.MIDISessionStopEvent;
import com.disappointedpig.midi.events.MIDISyncronizationCompleteEvent;
import com.disappointedpig.midi.events.MIDISyncronizationStartEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;
//import android.support.v7.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    static class ViewHolder {
        TextView text1;
        TextView text2;
        int position;
    }

    private IServiceFunctions service = null;

    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = (IServiceFunctions) binder;

            try {
                service.registerActivity(MainActivity.this, listener);
            } catch (Throwable t) {

            }
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }

    };

    SharedPreferences sharedpreferences;

    ToggleButton midiSessionToggle, cmServiceToggle, backgroundToggleButton, reconnectToggleButton;
    TextView midiStatusTextView;

    Button midiInviteButton, midiEndConnectionButton, openABButton;

    TextView midiConnectionStatusTextView;

    Button testheartbeat;

    boolean useReconnect = false;
//    ArrayList<MIDIDebugEvent> activityList=new ArrayList<MIDIDebugEvent>();

//    ArrayAdapter<MIDIDebugEvent> adapter;

//    private MIDISession midiSession;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        EventBus.getDefault().register(this);
        MIDISession.getInstance().init(DPMIDIApplication.getAppContext());

        //start cms
        Intent startIntent = new Intent(MainActivity.this, ConnectionManagerService.class);
        startIntent.setAction(Constants.ACTION.STARTCMGR_ACTION);
        startForegroundService(startIntent);

        bindToCMGRS();
        // bind to CMS
//        bindService(new Intent(this, ConnectionManagerService.class), svcConn, BIND_AUTO_CREATE);

        cmServiceToggle = (ToggleButton) findViewById(R.id.cmServiceToggleButton);

        midiInviteButton = (Button) findViewById(R.id.midiInviteButton);
        midiEndConnectionButton = (Button) findViewById(R.id.midiEndConnectionButton);
        backgroundToggleButton = (ToggleButton) findViewById(R.id.backgroundToggleButton);
        reconnectToggleButton = (ToggleButton) findViewById(R.id.reconnectToggleButton);

        midiSessionToggle = (ToggleButton) findViewById(R.id.midiSessionToggleButton);
        midiStatusTextView = (TextView) findViewById(R.id.midiStatus);

        midiConnectionStatusTextView = (TextView) findViewById(R.id.midiConnectionStatus);

        testheartbeat = (Button) findViewById(R.id.testheartbeat);
        testheartbeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ConnectionManager.GetInstance().testHeartbeat();
                Log.e("MAINActivity","should trigger test heartbeat");
            }
        });


        cmServiceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent startIntent = new Intent(MainActivity.this, ConnectionManagerService.class);
                    startIntent.setAction(Constants.ACTION.STARTCMGR_ACTION);
                    startForegroundService(startIntent);
                    // bind to CMS
                    bindToCMGRS();
                } else {
                    Intent startIntent = new Intent(MainActivity.this, ConnectionManagerService.class);
                    startIntent.setAction(Constants.ACTION.STOPCMGR_ACTION);
                    startForegroundService(startIntent);
                    unbindFromCMGRS();
                }
            }
        });

        backgroundToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);

                if (isChecked) {
                    ((DPMIDIApplication) getApplicationContext()).setRunInBackground(true);
//                    sharedpreferences.edit().putBoolean(Constants.PREF.MIDI_STATE_PREF, true).apply();
//                    sharedpreferences.edit().putBoolean(Constants.PREF.MIDI_STATE_PREF, true).apply();
                } else {
                    ((DPMIDIApplication) getApplicationContext()).setRunInBackground(false);
//                    sharedpreferences.edit().putBoolean(Constants.PREF.MIDI_STATE_PREF, false).apply();
//                    sharedpreferences.edit().putBoolean(Constants.PREF.MIDI_STATE_PREF, false).commit();
                }
            }
        });

        midiSessionToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);
                if (isChecked) {
                    sharedpreferences.edit().putBoolean(Constants.PREF.MIDI_STATE_PREF,true).commit();
                    Intent startIntent = new Intent(MainActivity.this, ConnectionManagerService.class);
                    startIntent.setAction(Constants.ACTION.START_MIDI_ACTION);
                    startForegroundService(startIntent);
                } else {
                    sharedpreferences.edit().putBoolean(Constants.PREF.MIDI_STATE_PREF,false).commit();
                    Intent startIntent = new Intent(MainActivity.this, ConnectionManagerService.class);
                    startIntent.setAction(Constants.ACTION.STOP_MIDI_ACTION);
                    startForegroundService(startIntent);
                }
            }
        });

        reconnectToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                MIDISession.getInstance().setAutoReconnect(isChecked);
                useReconnect = isChecked;
            }
        });

        midiInviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle rinfo = new Bundle();
                rinfo.putString(MIDIConstants.RINFO_ADDR,"192.168.1.178");
                rinfo.putInt(MIDIConstants.RINFO_PORT,5004);
                rinfo.putBoolean(MIDIConstants.RINFO_RECON, useReconnect);
                MIDISession.getInstance().connect(rinfo);
            }
        });

        midiEndConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle rinfo = new Bundle();
                rinfo.putString(MIDIConstants.RINFO_ADDR,"192.168.1.178");
                rinfo.putInt(MIDIConstants.RINFO_PORT,5004);
                rinfo.putBoolean(MIDIConstants.RINFO_RECON, useReconnect);
                MIDISession.getInstance().disconnect(rinfo);
            }
        });

        Button testMIDIButton = (Button) findViewById(R.id.testMIDIButton);
        testMIDIButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTestMIDI();
            }
        });

        openABButton = findViewById(R.id.openABButton);
        openABButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, AddressBook.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(intent, 1);
            }
        });
        SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);

//        if(service.cmsIsRunning() == true) {
        if(service != null) {
            cmServiceToggle.setChecked(service.cmsIsRunning());
            midiSessionToggle.setChecked(sharedpreferences.getBoolean(Constants.PREF.MIDI_STATE_PREF, false));
            backgroundToggleButton.setChecked(sharedpreferences.getBoolean(Constants.PREF.BACKGROUND_STATE_PREF, false));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);

//        if(service.cmsIsRunning() == true) {
        if(service != null) {
            cmServiceToggle.setChecked(service.cmsIsRunning());
            midiSessionToggle.setChecked(sharedpreferences.getBoolean(Constants.PREF.MIDI_STATE_PREF, false));
            backgroundToggleButton.setChecked(sharedpreferences.getBoolean(Constants.PREF.BACKGROUND_STATE_PREF, false));
        }
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Deactivate updates to us so that we dont get callbacks no more.
        service.unregisterActivity(this);

        // Finally stop the service
        unbindService(svcConn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void bindToCMGRS() {
        bindService(new Intent(this, ConnectionManagerService.class), svcConn, BIND_AUTO_CREATE);
    }

    public void unbindFromCMGRS() {
        service.unregisterActivity(this);
        unbindService(svcConn);
    }


    public void sendTestMIDI() {
        Log.d("Main","sendTestMidi");
        for (int i = 0; i < 128; i++) {
            Bundle testMessage = new Bundle();
            testMessage.putInt(MIDIConstants.MSG_COMMAND,0x09);
            testMessage.putInt(MIDIConstants.MSG_CHANNEL,0);
            testMessage.putInt(MIDIConstants.MSG_NOTE, i);
            testMessage.putInt(MIDIConstants.MSG_VELOCITY,127);
            MIDISession.getInstance().sendMessage(testMessage);
        }
//        MIDIMessage m = MIDIMessage.newUsing(testMessage);
//        MIDISession.getInstance().sendMessage(41,127);
//        MIDIMessage message = MIDISession.getInstance().sendNote(41,127);
//        if(message != null) {
//            MIDISession.getInstance().sendNote(41,127);
//        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDIConnectionEndEvent(MIDIConnectionEndEvent event) {
        Log.d("MainActivity","MIDIConnectionEndEvent");
        midiConnectionStatusTextView.setText("connection end");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDIConnectionEstablishedEvent(MIDIConnectionEstablishedEvent event) {
        Log.d("MainActivity","MIDIConnectionEstablishedEvent");
        midiConnectionStatusTextView.setText("connection start");

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDIConnectionRequestAcceptedEvent(MIDIConnectionRequestAcceptedEvent event) {
        Log.d("MainActivity","MIDIConnectionRequestAcceptedEvent");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDIConnectionRequestReceivedEvent(MIDIConnectionRequestReceivedEvent event) {
        Log.d("MainActivity","MIDIConnectionRequestReceivedEvent");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDIConnectionRequestRejectedEvent(MIDIConnectionRequestRejectedEvent event) {
        Log.d("MainActivity","MIDIConnectionRequestRejectedEvent");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMIDIReceivedEvent(MIDIReceivedEvent event) {
        Log.d("MainActivity","MIDIReceivedEvent");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDISessionNameRegisteredEvent(MIDISessionNameRegisteredEvent event) {
        Log.d("MainActivity","MIDISessionNameRegisteredEvent");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDISessionStartEvent(MIDISessionStartEvent event) {
        Log.d("MainActivity","MIDISessionStartEvent");
        midiStatusTextView.setText("running "+MIDISession.getInstance().version());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDISessionStopEvent(MIDISessionStopEvent event) {
        Log.d("MainActivity","MIDISessionStopEvent");
        midiStatusTextView.setText("stopped");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDISyncronizationCompleteEvent(MIDISyncronizationCompleteEvent event) {
        Log.d("MainActivity","MIDISyncronizationCompleteEvent");
        midiConnectionStatusTextView.setText("sync end");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDISyncronizationStartEvent(MIDISyncronizationStartEvent event) {
        Log.d("MainActivity","MIDISyncronizationStartEvent");
        midiConnectionStatusTextView.setText("sync start");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMIDIConnectionSentRequestEvent(MIDIConnectionSentRequestEvent event) {
        Log.d("MainActivity","MIDISyncronizationStartEvent");
        midiConnectionStatusTextView.setText("sent invite");
    }

    private IListenerFunctions listener = new IListenerFunctions() {
        public void midiStateChanged(ConnectionState state) {
            Log.d("MAIN","midistatechanged "+state.toString());
        }

        public void cmsStarted() {
            Log.d("MAIN","cms started ");
        }
    };

}


