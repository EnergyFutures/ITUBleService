ITUBleService
=============

The ITU BLE Service that will connect, collect and post measurements from sensors.

Android min. 4.3 is required due to Bluetooth 4.0 requirements

This is a proof of concept with some values hardcoded for now

The threading is not optimized

It is an android service that will auto start on boot, and is marked as STICKY (auto restart by system).

Service does the following:

- Loop the following: Start a BLE scan, sleep 5 min, stop the scan, sleep 25 min

- For each device found advertising

*******The below is done once on device connect********

-- If device name includes "ITU" then connect to it. This is mainly to the bug in filtered scanning

-- Start a service discovery

-- For each ITU SERVICE found in discovery

--- Change the config characteristic to: Show id + value

--- Ask for notifications of the value characteristic

*******The below is the basic loop********

- On each notification save the measurement

- If measurements count is more than 100, post for each id the measurements to SMAP, and empty list



Batched post packages are used to minimize the conflicts between the WIFI and BT

Thread pooling is used for worker tasks

![](https://raw.githubusercontent.com/EnergyFutures/ITUBleService/master/seq_dia.png)




