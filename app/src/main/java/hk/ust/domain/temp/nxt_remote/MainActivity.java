package hk.ust.domain.temp.nxt_remote;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import static android.view.MotionEvent.ACTION_BUTTON_PRESS;
import static android.view.MotionEvent.ACTION_BUTTON_RELEASE;

public class MainActivity extends AppCompatActivity {
    BluetoothBackend backend = null;
    SharedPreferences settings = null;
    Button up = null;
    Button down = null;
    Button left = null;
    Button right = null;
    Button stop = null;
    Button connect = null;
    Button disconnect = null;
    Button bend_right = null;
    Button bend_left = null;
    TextView textView = null;
    EditText motor_right_port;
    EditText motor_left_port;
    EditText motor_right_power_forward;
    EditText motor_right_power_backward;
    EditText motor_left_power_forward;
    EditText motor_left_power_backward;

    ServiceConnection conn = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothBackend.MyBinder myBinder =  (BluetoothBackend.MyBinder)service;
            backend = myBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            backend = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences("motor_map", 0);
        up = (Button)findViewById(R.id.up_btn);
        down = (Button)findViewById(R.id.down_btn);
        left = (Button)findViewById(R.id.left_btn);
        right = (Button)findViewById(R.id.right_btn);
        stop = (Button)findViewById(R.id.stop_btn);
        textView = (TextView)findViewById(R.id.state_txt);
        connect = (Button)findViewById(R.id.connect_btn);
        disconnect = (Button)findViewById(R.id.disconnect_btn);
        bend_right = (Button)findViewById(R.id.bend_right_btn);
        bend_left = (Button)findViewById(R.id.bend_left_btn);
        motor_right_port = (EditText)findViewById(R.id.motor_right);
        motor_left_port = (EditText)findViewById(R.id.motor_left);
        motor_right_power_forward = (EditText)findViewById(R.id.motor_right_power_forward);
        motor_right_power_backward = (EditText)findViewById(R.id.motor_right_power_backward);
        motor_left_power_forward = (EditText)findViewById(R.id.motor_left_power_forward);
        motor_left_power_backward = (EditText)findViewById(R.id.motor_left_power_backward);


    }
    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = new Intent();
        intent.setClassName("hk.ust.domain.temp.nxt_remote", "hk.ust.domain.temp.nxt_remote.BluetoothBackend");
        startService(intent);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        View.OnClickListener buttonListener = new View.OnClickListener(){
            public void onClick(View v) {
                checkService();
                if (!backend.connected()) {
                    textView.setText(getResources().getText(R.string.not_connected));
                    return;
                }
                if (v.equals(up)) {
                    disableButtons();
                    backend.up();
                    enableButtons();
                } else if (v.equals(down)) {
                    disableButtons();
                    backend.down();
                    enableButtons();
                } else if (v.equals(left)) {
                    disableButtons();
                    backend.left();
                    enableButtons();
                } else if (v.equals(right)) {
                    disableButtons();
                    backend.right();
                    enableButtons();
                } else if (v.equals(stop)) {
                    disableButtons();
                    backend.stop();
                    enableButtons();
                } else if(v.equals(bend_left)){
                    disableButtons();
                    backend.bendLeft();
                    enableButtons();
                } else if(v.equals(bend_right)){
                    disableButtons();
                    backend.bendRight();
                    enableButtons();
                }
                return;
            }

        };
        up.setOnClickListener(buttonListener);
        down.setOnClickListener(buttonListener);
        left.setOnClickListener(buttonListener);
        right.setOnClickListener(buttonListener);
        stop.setOnClickListener(buttonListener);
        bend_left.setOnClickListener(buttonListener);
        bend_right.setOnClickListener(buttonListener);
        final Activity mainActivity = this;
        connect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                class MyArrayAdapter extends ArrayAdapter{
                    private String[] str;
                    private Activity context;
                    private int res_id;
                    MyArrayAdapter(Activity c, int res_id, String[] str){
                        super(c, res_id, str);
                        this.str = str;
                        this.res_id = res_id;
                        context = (Activity)c;

                    }
                    @Override
                    public View getView(int position, View view, ViewGroup parent){
                        LayoutInflater inflater = context.getLayoutInflater();
                        View row = inflater.inflate(res_id, null, true);
                        TextView txt = (TextView) row.findViewById(R.id.list_item_txt);
                        txt.setText(str[position]);
                        return row;
                    }

                }
                checkService();
                backend.enableBluetooth();
                String[] pairedList = backend.getPairedList();
                ListView deviceList = new ListView(getApplicationContext());
                ArrayAdapter adapter = new MyArrayAdapter(mainActivity, R.layout.mylist_element, pairedList);
                deviceList.setAdapter(adapter);
                final PopupWindow p = new PopupWindow(deviceList, 400, 400, true);
                deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> a, View ParentView, int position, long id){
                        if(backend.connect(position)){
                            textView.setText("Connected!");
                        }
                        p.dismiss();
                    }
                });
                p.showAtLocation(findViewById(R.id.activity_main), Gravity.CENTER, 0, 0);

            }
        });
        disconnect.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                backend.disconnect();
                textView.setText("Disconnected!");
            }
        });
        TextView.OnEditorActionListener textListener = new TextView.OnEditorActionListener(){
            public boolean onEditorAction(TextView v, int action, KeyEvent event){
                if(action == EditorInfo.IME_ACTION_NEXT || action == EditorInfo.IME_ACTION_DONE) {
                    String key = "";
                    int min = 0;
                    int max = 0;
                    if (v.equals(motor_left_port)) {
                        key = "motorLeft";
                        min = 0;
                        max = 2;
                        motor_right_power_forward.requestFocus();
                    } else if (v.equals(motor_right_port)) {
                        key = "motorRight";
                        min = 0;
                        max = 2;
                        motor_left_port.requestFocus();
                    } else if (v.equals(motor_left_power_forward)) {
                        key = "motorLeftPowerForward";
                        min = -100;
                        max = 100;
                        motor_left_power_backward.requestFocus();
                    } else if (v.equals(motor_left_power_backward)) {
                        key = "motorLeftPowerBackward";
                        min = -100;
                        max = 100;
                        connect.requestFocus();
                    } else if (v.equals(motor_right_power_forward)) {
                        key = "motorRightPowerForward";
                        min = -100;
                        max = 100;
                        motor_right_power_backward.requestFocus();
                    } else if (v.equals(motor_right_power_backward)) {
                        key = "motorRightPowerBackward";
                        min = -100;
                        max = 100;
                        motor_left_power_forward.requestFocus();
                    }
                    SharedPreferences.Editor e = settings.edit();
                    int preference = Integer.parseInt(v.getText().toString());
                    if(preference < min){
                        preference = min;
                    }
                    if(preference > max){
                        preference = max;
                    }
                    v.setText("" + preference);
                    e.putInt(key, Integer.parseInt(v.getText().toString()));
                    e.commit();
                }
                return true;
            }
        };
        motor_left_port.setOnEditorActionListener(textListener);
        motor_right_port.setOnEditorActionListener(textListener);
        motor_left_power_forward.setOnEditorActionListener(textListener);
        motor_left_power_backward.setOnEditorActionListener(textListener);
        motor_right_power_forward.setOnEditorActionListener(textListener);
        motor_right_power_backward.setOnEditorActionListener(textListener);

        motor_left_port.setText("" + (settings.getInt("motorLeft", getResources().getInteger(R.integer.default_motor_left))));
        motor_right_port.setText("" + (settings.getInt("motorRight", getResources().getInteger(R.integer.default_motor_right))));
        motor_left_power_forward.setText("" + (settings.getInt("motorLeftPowerForward", getResources().getInteger(R.integer.default_motor_power))));
        motor_left_power_backward.setText("" + (settings.getInt("motorLeftPowerBackward", getResources().getInteger(R.integer.default_motor_power) * -1)));
        motor_right_power_forward.setText("" + (settings.getInt("motorRightPowerForward", getResources().getInteger(R.integer.default_motor_power))));
        motor_right_power_backward.setText("" + (settings.getInt("motorRightPowerBackward", getResources().getInteger(R.integer.default_motor_power) * -1)));




    }
    @Override
    protected void onPause(){
        super.onPause();
        unbindService(conn);
    }
    private void disableButtons(){
        setButtons(false);
    }
    private void enableButtons(){
        setButtons(true);
    }
    private void setButtons(boolean toggle){
        up.setActivated(toggle);
        down.setActivated(toggle);
        left.setActivated(toggle);
        right.setActivated(toggle);
        stop.setActivated(toggle);

    }
    private void checkService(){
        if(backend == null){
            Intent intent = new Intent();
            intent.setClassName("hk.ust.domain.temp.nxt_remote", "hk.ust.domain.temp.nxt_remote.BluetoothBackend");
            startService(intent);
            bindService(intent, conn, Context.BIND_AUTO_CREATE);
        }
    }

}
