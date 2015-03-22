package dk.itu.energyfutures.ble.task;

import dk.itu.energyfutures.ble.DoneEmptyingBufferListner;

public interface DoneEmptyingBufferNotifer {
	void registerDoneEmptyingBufferListner(DoneEmptyingBufferListner listner);
}
