package com.example.minibluetooth;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	private static final String TAG = "MainActivity";
    private ProgressDialog progressDialog;

    private Button on, off, paired, scan, send;
	private ListView foundDevices;
	private ImageView image;
	private ArrayAdapter<String> bluetoothArrayAdapter;
	private ArrayList<BluetoothDevice> devices;	
	private  BroadcastReceiver bluetoothReceiver;
    private BluetoothAdapter adapter;
    private Set<BluetoothDevice> pairedDevices;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bluetoothConfiguration();
		bluetoothReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Toast.makeText(getApplicationContext(), "Discovering Devices", Toast.LENGTH_SHORT).show();
				//for discovering device
				String action = intent.getAction();				
				if(BluetoothDevice.ACTION_FOUND.equals(action))
				{
					//getting bluetooth device from intent
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					//adding device to array_list and bluetooth_adapter
					devices.add(device);
					bluetoothArrayAdapter.add(device.getName()+ "\n"+ device.getAddress());
					bluetoothArrayAdapter.notifyDataSetChanged();
				}
			}
		};		
		
        MainApplication.clientHandler = new Handler() 
        {
            @Override
            public void handleMessage(Message message) 
            {
                switch (message.what)
                {
                    case MessageType.READY_FOR_DATA: 
                    {
//--------------------------------------------------------------------------------------------------------->>
                    	Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        File file = new File(Environment.getExternalStorageDirectory(), MainApplication.TEMP_IMAGE_FILE_NAME);
                        Uri outputFileUri = Uri.fromFile(file);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                        startActivityForResult(takePictureIntent, MainApplication.PICTURE_RESULT_CODE);
//--------------------------------------------------------------------------------------------------------->>
                        break;
                    }

                    case MessageType.COULD_NOT_CONNECT:
                    {
                        Toast.makeText(MainActivity.this, "Unable to connect to the paired device", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    case MessageType.SENDING_DATA: 
                    {
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("Sending...");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.show();
                        break;
                    }

                    case MessageType.DATA_SENT_OK:
                    {
                        if (progressDialog != null) 
                        {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }
                        Toast.makeText(MainActivity.this, "Data sent successfully", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    case MessageType.DIGEST_DID_NOT_MATCH: 
                    {
                        Toast.makeText(MainActivity.this, "Data was sent incorrectly", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        };

        MainApplication.serverHandler = new Handler()
        {
            @Override
            public void handleMessage(Message message)
            {
                switch (message.what) 
                {
                    case MessageType.DATA_RECEIVED: 
                    {
                        if (progressDialog != null) 
                        {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }
//--------------------->>
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        Bitmap image = BitmapFactory.decodeByteArray(((byte[]) message.obj), 0, ((byte[]) message.obj).length, options);
                        ImageView imageView = (ImageView) findViewById(R.id.imageView);
                        imageView.setImageBitmap(image);
//--------------------->>
                        break;
                    }

                    case MessageType.DIGEST_DID_NOT_MATCH: 
                    {
                        Toast.makeText(MainActivity.this, "Data was received incorrectly", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    case MessageType.DATA_PROGRESS_UPDATE:
                    {
                        // some kind of update
                        MainApplication.progressData = (ProgressData) message.obj;
                        double pctRemaining = 100 - (((double) MainApplication.progressData.remainingSize / MainApplication.progressData.totalSize) * 100);
                        if (progressDialog == null) 
                        {
                            progressDialog = new ProgressDialog(MainActivity.this);
                            progressDialog.setMessage("Receiving...");
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setProgress(0);
                            progressDialog.setMax(100);
                            progressDialog.show();
                        }
                        progressDialog.setProgress((int) Math.floor(pctRemaining));
                        break;
                    }

                    case MessageType.INVALID_HEADER: 
                    {
                        Toast.makeText(MainActivity.this, "Data was sent incorrectly", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        };
		
		
		on.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				on();
			}			
		});
		off.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				off();
			}			
		});
		paired.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				availablePaired();
			}			
		});
		scan.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				scan();
			}			
		});
		send.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				send();
			}
		});
				
	}		
		
//***************************************************************************************************************************************************//		

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MainApplication.PICTURE_RESULT_CODE)
        {
            if (resultCode == RESULT_OK) 
            {
                Log.v(TAG, "Photo acquired from camera intent");
                try{
//--------------------->>
                    File file = new File(Environment.getExternalStorageDirectory(), MainApplication.TEMP_IMAGE_FILE_NAME);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                    ByteArrayOutputStream compressedImageStream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, MainApplication.IMAGE_QUALITY, compressedImageStream);
                    byte[] compressedImage = compressedImageStream.toByteArray();
//--------------------->>
                    Log.v(TAG, "Compressed image size: " + compressedImage.length);

                    // Invoke client thread to send
                    Message message = new Message();
                    message.obj = compressedImage;
                    MainApplication.clientThread.incomingHandler.sendMessage(message);

                    // Display the image locally
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    imageView.setImageBitmap(image);

                } 
                catch (Exception e) 
                {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }
	
    @Override
    protected void onStop() 
    {
        super.onStop();
        if (progressDialog != null) 
        {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	//------------------------------- Available Devices -----------------------------//	
		public void availablePaired()
		{
			bluetoothArrayAdapter.clear();
			adapter.cancelDiscovery();
			//this will give us the paired devices list
			pairedDevices = adapter.getBondedDevices();
			for(BluetoothDevice device : pairedDevices)
			{	bluetoothArrayAdapter.add(device.getName()+ "\n"+ device.getAddress());		}				
		}

		
	//------------------------------- Turning On Bluetooth --------------------------//	
		public void on()
		{
			//------------------ Checking bluetooth enable status		
			//if bluetooth is not enabled then enabling it
			if(!adapter.isEnabled())
			{
				Intent enableBluetoothIntent = new Intent(adapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBluetoothIntent,1);//1 : ENABLE BLUETOOTH
				Toast.makeText(getApplicationContext(),"Turned On",Toast.LENGTH_LONG).show();
			}
			else
			{	Toast.makeText(getApplicationContext(),"Already on",Toast.LENGTH_LONG).show();		}								
		}
		
		
	//------------------------------ Turning Off Bluetooth --------------------------//	
		public void off()
		{
			//------------------ Checking off button status
			bluetoothArrayAdapter.clear();
			adapter.disable();
			Toast.makeText(getApplicationContext(),"Turned Off",Toast.LENGTH_LONG).show();
		}
		
	//----------------------------- Pairing Devices ---------------------------------//
		private Boolean pairDevice(BluetoothDevice device)
		{
			try
			{
				Method method = device.getClass().getMethod("createBond", (Class[]) null);
				method.invoke(device, (Object[]) null);
				return true;
			}
			catch(Exception ec)
			{
				return false;
			}
		}
		//-------------------------------- Scanning Devices ------------------------------//	
		public void scan()
		{
			//this will give us the paired devices list
			pairedDevices = adapter.getBondedDevices();

			bluetoothArrayAdapter.clear();
			adapter.startDiscovery();
			registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		
			foundDevices.setOnItemClickListener(new OnItemClickListener()
			{
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
				{
					//If bluetooth is discovering devices then cancelling it because device is selected
					if(adapter.isDiscovering())
					{	adapter.cancelDiscovery();		}
					
					//getting bluetooth device
					BluetoothDevice selected = devices.get(position);
					
					//If paired devices have selected device
					if(pairedDevices.contains(selected))
					{
						Toast.makeText(getApplicationContext(), "Device is already Paired", Toast.LENGTH_LONG).show();						
					}
					//else making it a pair device
					else
					{
						if(pairDevice(selected))
							Toast.makeText(getApplicationContext(), "Device Paired", Toast.LENGTH_LONG).show();
						
						else
							Toast.makeText(getApplicationContext(), "Problem in pairing", Toast.LENGTH_LONG).show();
					}
					
				}
			});

		}
		//--------------------- Bluetooth Configuration Setting -------------------------//
		public void bluetoothConfiguration()
		{
			//bluetothAdapter connection
			adapter = BluetoothAdapter.getDefaultAdapter();

			//Button linkage setting
			on = (Button) findViewById(R.id.on);
			off = (Button) findViewById(R.id.off);
			scan = (Button) findViewById(R.id.scan);
			paired = (Button) findViewById(R.id.pairedDevice);
			foundDevices = (ListView) findViewById(R.id.listView);
			send = (Button) findViewById(R.id.send);
			image = (ImageView) findViewById(R.id.imageView);
			
			//for displaying the bluetooth devices
			devices = new ArrayList<BluetoothDevice>();
			bluetoothArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
			foundDevices.setAdapter(bluetoothArrayAdapter);

			//if blueoothAdapter is null then device don't have bluetooth
			if(adapter == null)
			{	Toast.makeText(getApplicationContext(),"Bluetooth Not Found",Toast.LENGTH_LONG).show();		}	
		}
	
		//---------------------- Sending of Data ------------------------------------------//
		public void send()
		{
			availablePaired();
    		Toast.makeText(getApplicationContext(),"Select Device",Toast.LENGTH_LONG).show();
			foundDevices.setOnItemClickListener(new OnItemClickListener()
    		{
    			@Override
    			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    			{
    				//getting bluetooth device
    				BluetoothDevice device = devices.get(position);
    				MainApplication.clientThread = new ClientThread(device, MainApplication.clientHandler);
    				//MainApplication.clientThread.connect();
    				Toast.makeText(getApplicationContext(), "Device Connected", Toast.LENGTH_LONG).show();
//-----------------------------------------------------------------------------------------------//
    				
    				FileDialog fileDialog;
    				
    				File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
    	            
    				fileDialog = new FileDialog(MainActivity.this, mPath);
    				Toast.makeText(getApplicationContext(),"File Dialog",Toast.LENGTH_LONG).show();
    				
    				fileDialog.setFileEndsWith(".txt");
    	            
    				fileDialog.addFileListener(new FileDialog.FileSelectedListener() 
    				{
    	            	public void fileSelected(File file) 
    	            	{
    	                    Log.d(getClass().getName(), "selected file " + file.toString());
    	                    Toast.makeText(getApplicationContext(),"File Selected",Toast.LENGTH_LONG).show();
    	            	}
    	            });
    	            fileDialog.showDialog();
    	            
//-----------------------------------------------------------------------------------------------//    	            

    			}			
    		});
			
		}
}
