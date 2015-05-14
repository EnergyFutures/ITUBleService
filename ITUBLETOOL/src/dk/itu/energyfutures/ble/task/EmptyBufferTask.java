package dk.itu.energyfutures.ble.task;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import dk.itu.energyfutures.ble.Application;
import dk.itu.energyfutures.ble.helpers.GattAttributes;
import dk.itu.energyfutures.ble.helpers.ITUConstants;
import dk.itu.energyfutures.ble.smap.SMAPController;

public class EmptyBufferTask implements Runnable, TaskDoneNotifer {
	private final static String TAG = EmptyBufferTask.class.getSimpleName();
	private BluetoothDevice device;
	private BluetoothGattDescriptor readAllDescriptor;
	private BluetoothGattCharacteristic readAllChar;
	private Context context;
	private List<TaskDoneListner> doneEmptyingBufferListners = new ArrayList<TaskDoneListner>();
	private boolean done;
	private int pointer = 0;
	private byte[] values = new byte[55000];
	private BluetoothGatt bleGatt;
	private static final int WAIT_TIME = 1 * 60 * 1000;
	private static final int THREAD_SLEEP = 1 * 1000;
	private long timeOfLastActivity = System.currentTimeMillis();

	public BluetoothDevice getDevice() {
		return device;
	}

	public void setDevice(BluetoothDevice device) {
		this.device = device;
	}

	@Override
	public void run() {
		device.connectGatt(context, false, gattCallback);
		Thread thisThread = Thread.currentThread();
		try {
			while (!done) {
				if(System.currentTimeMillis() - timeOfLastActivity > WAIT_TIME){
					break;
				}else if(thisThread.isInterrupted()){
					break;
				}
				Thread.sleep(THREAD_SLEEP);
			}
			if((pointer % 8) != 0){
				Log.e(TAG,"READINGS MALFORMED");
			}else{
				SMAPController.postReadingsToSmap(values, pointer);
				Log.i(TAG,"SENDING E_MAIL");
				StringBuffer sb = new StringBuffer();
				sb.append("Device: " + device.getAddress());
				sb.append("\nName: " + device.getName());
				sb.append("\nNumber of values: " + (pointer / 8));
				while(SMAPController.payload == null){
					
				}
				sb.append("\nPayload: " + SMAPController.payload);
				SMAPController.payload = null;
				sendMail(sb.toString());
				Log.i(TAG,"E_MAIL SENT");
			}
			closeDown("Done... disconnecting");
		}
		catch (Exception e) {
			closeDown("Exception handled... disconnecting");
		}finally{
			for (TaskDoneListner listner : doneEmptyingBufferListners) {
				listner.onTaskDone(device.getAddress());
			}	
			Log.v(TAG, "Number of received  bytes: " + pointer);
		}
	}
	
	private void closeDown(String msg) {
		if (bleGatt != null) {
			bleGatt.disconnect();
			Log.v(TAG, msg);
			try {
				Thread.sleep(250);
			}
			catch (InterruptedException ex) {}
			bleGatt.close();
		}
	}

	private Session createSessionObject() {
	    Properties properties = new Properties();
	    properties.put("mail.smtp.auth", "true");
	    properties.put("mail.smtp.starttls.enable", "true");
	    properties.put("mail.smtp.host", "smtp.gmail.com");
	    properties.put("mail.smtp.port", "587");
	 
	    return Session.getInstance(properties, new javax.mail.Authenticator() {
	        protected PasswordAuthentication getPasswordAuthentication() {
	            return new PasswordAuthentication("bleot.4d21@gmail.com", "Zaq12wsx12");
	        }
	    });
	}
	
	private Message createMessage(String subject, String messageBody, Session session) throws MessagingException, UnsupportedEncodingException {
	    Message message = new MimeMessage(session);
	    message.setFrom(new InternetAddress("bleot.4d21@gmail.com", "ITU 4D21"));
	    message.addRecipient(Message.RecipientType.TO, new InternetAddress("bleot.4d21@gmail.com", "ITU 4D21"));
	    message.setSubject(subject);
	    message.setText(messageBody);
	    return message;
	}
	
	private void sendMail(String messageBody) {
	    Session session = createSessionObject();
	    try {
	        Message message = createMessage("OFF-LOADING", messageBody, session);
	        Transport.send(message);
	    } catch (AddressException e) {
	        e.printStackTrace();
	    } catch (MessagingException e) {
	        e.printStackTrace();
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }
	}
	
	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
			String adr = gatt.getDevice().getAddress();
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i(TAG, "Connected to GATT server: " + adr);
				gatt.discoverServices();
				timeOfLastActivity = System.currentTimeMillis();
				bleGatt = gatt;
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				Log.v(TAG, "Disconnecting to GATT server: " + adr);
				done = true;
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.v(TAG, "Disconnected to GATT server: " + adr);
				done = true;
			} else if (newState == BluetoothProfile.STATE_CONNECTING) {
				Log.v(TAG, "Connecting to GATT server: " + adr);
				timeOfLastActivity = System.currentTimeMillis();
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
			String adr = gatt.getDevice().getAddress();
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Done discovering: " + adr);
				timeOfLastActivity = System.currentTimeMillis();
				BluetoothGattService service = gatt.getService(ITUConstants.BLE_UUID_ITU_MOTE_SERVICE);
				if(service == null){
					Log.e(TAG, "Service is null");
					gatt.disconnect();
					done = true;
					return;
				}
				readAllChar = service.getCharacteristic(ITUConstants.BLE_UUID_ITU_READ_ALL_MEASUREMENTS_VALUE_CHAR);
				readAllDescriptor = readAllChar.getDescriptor(GattAttributes.BLE_UUID_CCCD_DESCRIPTOR);
				Log.v(TAG, "enable notifications: " + gatt.setCharacteristicNotification(readAllChar, true));
				readAllDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
				gatt.writeDescriptor(readAllDescriptor);
				Application.showShortToastOnUI("Off-loading device: " + adr);
			} else {
				Log.e(TAG, "onServicesDiscovered received: " + status + " for adr: " + adr);
			}
		}

		@Override
		public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.v(TAG, "Received packet: " + characteristic.getValue());
			}
			timeOfLastActivity = System.currentTimeMillis();
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.v(TAG, "onCharacteristicChanged adr: " + gatt.getDevice().getAddress());
			timeOfLastActivity = System.currentTimeMillis();
			byte[] receivedBytes = characteristic.getValue();
			if (receivedBytes.length == 1 && receivedBytes[0] == 0 ) {
				Log.i(TAG, "Received termination package, disable notification");
				gatt.setCharacteristicNotification(readAllChar, false);
				readAllDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
				gatt.writeDescriptor(readAllDescriptor);
			} else {
				Log.v(TAG, "Received data packages");
				for (int i = 0; i < receivedBytes.length; i++) {
					values[pointer++] = receivedBytes[i];
				}
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + characteristic.getUuid());
			timeOfLastActivity = System.currentTimeMillis();
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.v(TAG, "onDescriptorRead received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + descriptor.getUuid());
			timeOfLastActivity = System.currentTimeMillis();
		};

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.v(TAG, "onDescriptorWrite received: " + status + " for adr: " + gatt.getDevice().getAddress());
			Log.v(TAG, "And characteristic: " + descriptor.getUuid());
			timeOfLastActivity = System.currentTimeMillis();
			if (descriptor.getValue()[0] == 0 && descriptor.getValue()[1] == 0) {
				Log.i(TAG, "Descriptor reset.. we should now disconnect");
				Application.showShortToastOnUI("Done off-loading device: " + gatt.getDevice().getAddress() + ". Received bytes: " + pointer);
				gatt.disconnect();
				done = true;
			}
		};

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {};

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			Log.v(TAG, "onReliableWriteCompleted received: " + status + " for adr: " + gatt.getDevice().getAddress());
			timeOfLastActivity = System.currentTimeMillis();
		};
	};

	public void setContext(Context applicationContext) {
		this.context = applicationContext;
	}

	@Override
	public void registerTaskDoneListner(TaskDoneListner listner) {
		doneEmptyingBufferListners.add(listner);
	}
}
