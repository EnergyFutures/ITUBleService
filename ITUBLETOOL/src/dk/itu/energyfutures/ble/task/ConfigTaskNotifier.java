package dk.itu.energyfutures.ble.task;

import java.io.UnsupportedEncodingException;
import java.util.List;

import dk.itu.energyfutures.ble.sensorhandlers.MoteConfigParser;
import dk.itu.energyfutures.ble.sensorhandlers.SensorParser;

public interface ConfigTaskNotifier {
	void registerListner(ConfigTaskListner listner);
	void unregisterListner(ConfigTaskListner listner);
	void writeConfigAndExit(MoteConfigParser moteConfig, List<SensorParser> sensorParsers) throws UnsupportedEncodingException;
}
