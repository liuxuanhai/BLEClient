package com.zjw.bluetooth;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.util.ByteArrayBuffer;

import android.R.string;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private Button sendButton ;
	private Button connectButton;
	private TextView outpuTextView;
	private EditText inputEditText;
	private ScrollView scrollView;
	private ListView listView;
	private String outString = new String();
	private ArrayAdapter<String> arrayAdapter= null;
	private boolean findDeviceSuccess =false;
	private boolean bleScanning = false;
	private final UUID SERVICE_UUID = 	UUID.fromString("0003cdd0-0000-1000-8000-00805f9b0131");
	private final UUID RX_UUID = 		UUID.fromString("0003cdd1-0000-1000-8000-00805f9b0131");
	private final UUID TX_UUID = 		UUID.fromString("0003cdd2-0000-1000-8000-00805f9b0131");
	private final String TAG = "BLE_DEMO";
	private BluetoothManager bluetoothManager = null;
	private BluetoothAdapter bluetoothAdapter = null;
	private BluetoothDevice bluetoothDevice = null;
	private BluetoothGatt bluetoothGatt = null;
	private BluetoothGattCharacteristic btCharacteristicTX = null;
	private BluetoothGattCharacteristic btCharacteristicRX = null;
	private BluetoothGattDescriptor btDescriptor = null;
	private BluetoothGattService bluetoothGattService = null;
	private Handler printHandler = new Handler();
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		sendButton = (Button)findViewById(R.id.sendButton);
		connectButton = (Button)findViewById(R.id.connectButton);
		outpuTextView = (TextView)findViewById(R.id.outputView);
		inputEditText = (EditText)findViewById(R.id.inputView);
		scrollView = (ScrollView)findViewById(R.id.scrollView);
		listView = (ListView)findViewById(R.id.listView);
		arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		
		listView.setAdapter(arrayAdapter);
//		listView.setOnItemClickListener(itemClickListener);
		sendButton.setOnClickListener(listener);
		connectButton.setOnClickListener(listener);
//		sendButton.setEnabled(false);
		bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
	}
	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
			
			if(id<0){
				return;
			}
			printTextView("item int"+position+"\n");
			
		}
	};
	//定义监听器
	private OnClickListener listener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch(v.getId()){
			
			case R.id.sendButton:
				
				String str = inputEditText.getText().toString();
				if(str.length()==0){
					str = "please input data!";
				}else{
					new writeThread(str).start();
//					new readThread().start();
				}
//				printTextView(str+"\n");
				//自动滚屏
//				scrollView.post(runAble);
				break;
			case R.id.connectButton:
				if(findDeviceSuccess == true){
					printTextView("already find!\n");
					break;
				}
				//如果不支持BLE 退出
				if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
				    Toast.makeText(MainActivity.this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
				    finish();
				}
				//开启本地蓝牙
				if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
					Intent enBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivity(enBluetoothIntent);		
				}
				scanLeDevice(true);
				bluetoothAdapter.startLeScan(leScanCallback);
				break;
			default:break;
			}
		}
	}; 
	private void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run() {
                	bleScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, 10000);

            bleScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            bleScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
	//LE 扫描回调函数
	private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback(){

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {	
			
			Log.d(TAG,device.getName()+"");
			if(device.getName().equals("ADS_BLE_SIREN")){
				Log.d(TAG,"BLE device find! ");
				bluetoothDevice = device;
				findDeviceSuccess = true;
				
				new bleCtThread().start();
			}
		}		
	};
	
	public void printTextView(String string){
		outString += string;
		outpuTextView.setText(outString);
		scrollView.post(new Runnable(){
				public void run() {
					scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
			
	}
	public void showDeviceList(String string){
        arrayAdapter.add(string);
		arrayAdapter.notifyDataSetChanged();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {	
		if(bleScanning)
			bluetoothAdapter.stopLeScan(null);
		super.onDestroy();
	}
	//蓝牙通讯子线程
	class bleCtThread extends Thread{			
		private bleCtThread(){
			bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,false,bluetoothGattCallback);
			if(bluetoothGatt != null){
				Log.d(TAG, "get bluetooth gatt!");
			}
		}
		public void run(){		
			if(bluetoothGatt == null)
				return;
			bluetoothGatt.connect();
		}
	}
	private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.d(TAG, "BLE sevices discovered result!");
			bluetoothGattService = bluetoothGatt.getService(SERVICE_UUID);
			List<BluetoothGattService> services = bluetoothGatt.getServices();
			for(BluetoothGattService service :services){
				
				Log.d(TAG, "service:"+service.getUuid().toString()+"\n");
				List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
				for(BluetoothGattCharacteristic characteristic :characteristics){
					
					Log.d(TAG, "service:"+characteristic.getUuid().toString()+"\n"+characteristic.getProperties());
				}
			}
			if(bluetoothGattService != null){
				List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
				for(BluetoothGattCharacteristic characteristic :characteristics){
					
					Log.d(TAG, ""+characteristic.getUuid().toString()+"\n"+characteristic.getProperties());
				}
				//通过Service获得Characteristic_TX
				btCharacteristicTX = bluetoothGattService.getCharacteristic(TX_UUID);
				//通过Service获得Characteristic_RX
				btCharacteristicRX = bluetoothGattService.getCharacteristic(RX_UUID);
//				Log.d(TAG, "tx type"+":"+btCharacteristicTX.getProperties()+":"+BluetoothGattCharacteristic.PROPERTY_WRITE);
				Log.d(TAG, "rx type"+":"+btCharacteristicRX.getProperties()+":"+BluetoothGattCharacteristic.PROPERTY_READ);
				List<BluetoothGattDescriptor> btDescriptors = btCharacteristicRX.getDescriptors();
				for(BluetoothGattDescriptor descriptor :btDescriptors){	
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					bluetoothGatt.writeDescriptor(descriptor);
					Log.d(TAG, "descriptor:"+descriptor.getUuid().toString());
				}
				if(bluetoothGatt.setCharacteristicNotification(btCharacteristicRX, true)){
					Log.d(TAG, "setCharacteristicNotification success!");
				}
			}else {
				Log.d(TAG, "can not get service!");
			}
			super.onServicesDiscovered(gatt, status);
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,int newState) {
			Log.d(TAG, "BLE Connection State Change!");
			String str = null;
			if(newState == BluetoothGatt.STATE_CONNECTED){
				bluetoothGatt.discoverServices();
				
				str = "BLE state CONNECTED!";
			}else if(newState == BluetoothGatt.STATE_CONNECTING){
				str = "BLE state CONNECTING!";
			}else if(newState == BluetoothGatt.STATE_DISCONNECTED){
				bluetoothGatt.close();
				btCharacteristicTX = null;
				btCharacteristicRX = null;
				bluetoothGatt = null;
				findDeviceSuccess = false;
				str = "BLE state DISCONNECTED!";
			}else if(newState == BluetoothGatt.STATE_DISCONNECTING){
				str = "BLE state DISCONNECTING!";
			}else{
				str = "BLE state none";
				}
			Log.d(TAG, str);
			super.onConnectionStateChange(gatt, status, newState);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic) {
			Log.d(TAG, "BLE Characteristic Changed!"+characteristic.getUuid().toString());
			printHandler.post(new Runnable() {
				String str = Utils.bytesToHexString(btCharacteristicRX.getValue());
				@Override
				public void run() {
					String line = str;
					printTextView(str + "\n");
				}
			});
			
			super.onCharacteristicChanged(gatt, characteristic);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "characteristic write complete"+characteristic.getUuid().toString());
			super.onCharacteristicWrite(gatt, characteristic, status);
		}

	};
	//发送数据线程
	class writeThread extends Thread{
		String string = null;
		private writeThread(String str){	
			string = str;
		}
		public void run(){
			byte[] arm = {0x0C,0x15,0x13,0x1d,0x00,0x15+0x13+0x1d,0x0d};
			
			String line = new String(arm);
			if(btCharacteristicTX != null){
				btCharacteristicTX.setValue(line);
				bluetoothGatt.writeCharacteristic(btCharacteristicTX);
			}else{
				Log.d(TAG, "btCharacteristicTX is empty!");
			}
		}
	}
}
