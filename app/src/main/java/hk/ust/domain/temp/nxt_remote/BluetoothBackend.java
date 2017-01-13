package hk.ust.domain.temp.nxt_remote;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

public class BluetoothBackend extends Service {
    private BluetoothSocket socket = null;
    private OutputStream ostream = null;
    private BluetoothDevice activeDevice = null;
    private LinkedList<BluetoothDevice> devices = null;
    private MyBinder binder = new MyBinder();
    private SharedPreferences settings = null;
    int motorLeft;
    int motorRight;
    int motorLeftPowerForward;
    int motorRightPowerForward;
    int motorLeftPowerBackward;
    int motorRightPowerBackward;
    public BluetoothBackend() {
        super();

    }
    public class MyBinder extends Binder {
        public BluetoothBackend getService(){
            return BluetoothBackend.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");

        return binder;
    }
    @Override
    public void onCreate(){
        super.onCreate();
        settings = getSharedPreferences("motor_map", 0);
    }

    public boolean connected() {
        if(socket == null){
            return false;
        }
        return socket.isConnected();
    }
    public boolean bluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter.isEnabled();
    }
    public boolean enableBluetooth(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(!adapter.isEnabled()) {
            adapter.enable();
            while (!adapter.isEnabled()) {
            }
        }
        return true;
    }
    public boolean connect(int i) {
        getPairedList();
        if(i > devices.size() - 1 || i < 0) {
            return false;
        }
        if(connected()){
            return true;
        }
        activeDevice = devices.get(i);
        try {
            disconnect();
            socket = activeDevice.createRfcommSocketToServiceRecord(UUID
                    .fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    public boolean disconnect(){
        if(socket != null){
            try {
                socket.close();
                socket = null;
            }catch(IOException e){
                return false;
            }
        }
        return true;
    }
    public boolean sendCommand(byte[] command) {
        if(!connected()) {
            return false;
        }
        try {
            if(ostream == null){
                ostream = socket.getOutputStream();
            }
            ostream.write(command);
        }catch(IOException e){
            socket = null;
            return false;
        }

        return true;
    }
    public String[] getPairedList(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        this.devices = new LinkedList<BluetoothDevice>();
        String[] result = new String[devices.size()];
        int i = 0;
        for(BluetoothDevice device : devices){
            this.devices.add(device);
            result[i] = device.getName();
            i++;
        }
        return result;
    }
    // wrappers for sendCommand
    private byte[] sharedCommands(){
        motorLeft = settings.getInt("motorLeft", getResources().getInteger(R.integer.default_motor_left));
        motorRight = settings.getInt("motorRight", getResources().getInteger(R.integer.default_motor_right));
        motorLeftPowerForward = settings.getInt("motorLeftPowerForward", getResources().getInteger(R.integer.default_motor_power));
        motorRightPowerForward = settings.getInt("motorRightPowerForward", getResources().getInteger(R.integer.default_motor_power));
        motorLeftPowerBackward = settings.getInt("motorLeftPowerBackward", getResources().getInteger(R.integer.default_motor_power) * -1);
        motorRightPowerBackward = settings.getInt("motorRightPowerBackward", getResources().getInteger(R.integer.default_motor_power) * -1);

        byte[] command = new byte[15];
        // message length, lsB
        command[0] = (byte)13;
        // message length, msB
        command[1] = (byte)0;
        // 0x00 for receiving replies, 0x80 for don't
        command[2] = (byte)0x80;
        // 0x04 is the code for 'set output state'
        command[3] = (byte)0x04;
        // port 0x00~0x02, 0xFF is all motors
        //command[4] = (byte)0x00;
        // setting power, -100 to 100
        //command[5] = (byte)0;
        // mode, 0x01 to turn on the motor, 0x02 to enable break, 0x04 to enable regulations, bitfield
        command[6] = (byte)(0x01 | 0x02 | 0x04);
        // regulation mode, 0x00 idle, 0x01 power control, 0x02 sync
        command[7] = (byte)0x01;
        // turn ratio, for sync regulation mode, -100% to 100%
        command[8] = (byte)0x00;
        // runstate, 0x00 idle, 0x10 rampup, 0x20 running, 0x40 rampdown
        command[9] = (byte)0x20;
        // 'TachoLimit', 0 runs forever
        command[10] = (byte)0;
        command[11] = (byte)0;
        command[12] = (byte)0;
        command[13] = (byte)0;
        command[14] = (byte)0;
        return command;
    }
    public void up(){
        byte[] command = sharedCommands();

        command[4] = (byte)motorRight;
        command[5] = (byte)motorRightPowerForward;
        sendCommand(command);
        command[4] = (byte)motorLeft;
        command[5] = (byte)motorLeftPowerForward;
        sendCommand(command);
    }
    public void down(){
        byte[] command = sharedCommands();

        command[4] = (byte)motorRight;
        command[5] = (byte)motorRightPowerBackward;
        sendCommand(command);
        command[4] = (byte)motorLeft;
        command[5] = (byte)motorLeftPowerBackward;
        sendCommand(command);
    }
    public void right(){
        byte[] command = sharedCommands();

        command[4] = (byte)motorRight;
        command[5] = (byte)motorRightPowerBackward;
        sendCommand(command);
        command[4] = (byte)motorLeft;
        command[5] = (byte)motorLeftPowerForward;
        sendCommand(command);
    }
    public void bendLeft(){
        byte[] command = sharedCommands();

        command[4] = (byte)motorRight;
        command[5] = (byte)motorRightPowerForward;
        sendCommand(command);
        command[4] = (byte)motorLeft;
        command[5] = (byte)Math.floor(motorLeftPowerForward * 0.5);
        sendCommand(command);
    }
    public void bendRight(){
        byte[] command = sharedCommands();

        command[4] = (byte)motorRight;
        command[5] = (byte)Math.floor(motorRightPowerForward * 0.5);
        sendCommand(command);
        command[4] = (byte)motorLeft;
        command[5] = (byte)motorLeftPowerForward;
        sendCommand(command);
    }
    public void left(){
        byte[] command = sharedCommands();

        command[4] = (byte)motorRight;
        command[5] = (byte)motorRightPowerForward;
        sendCommand(command);
        command[4] = (byte)motorLeft;
        command[5] = (byte)motorLeftPowerBackward;
        sendCommand(command);
    }
    public void stop(){
        byte[] command = sharedCommands();

        command[4] = (byte)motorRight;
        command[5] = (byte)0;
        sendCommand(command);
        command[4] = (byte)motorLeft;
        command[5] = (byte)0;
        sendCommand(command);
    }
}