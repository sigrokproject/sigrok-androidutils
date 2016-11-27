/*
 * This file is part of the sigrok-androidutils project.
 *
 * Copyright (C) 2016 Marcus Comstedt <marcus@mc.pp.se>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sigrok.androidutils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

final class UsbEventMonitor
{
	private final Context context;
	private final UsbManager manager;
	private final UsbEventListener listener;
	private final BroadcastReceiver permReceiver;
	private final BroadcastReceiver hotplugReceiver;
	private final IntentFilter permFilter;
	private final IntentFilter hotplugFilter;

	UsbEventMonitor(Context context, UsbManager manager, UsbEventListener listener)
	{
		this.context = context;
		this.manager = manager;
		this.listener = listener;
		permReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (UsbSupplicant.ACTION_USB_PERMISSION.equals(action)) {
					permissionCallback((UsbDevice)intent.getParcelableExtra(
						UsbManager.EXTRA_DEVICE), intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED, false));
				}
			}
		};
		hotplugReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent != null && UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
					attachCallback((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
				} else if (intent != null && UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
					detachCallback((UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
				}
			}
		};
		permFilter = new IntentFilter(UsbSupplicant.ACTION_USB_PERMISSION);
		hotplugFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		hotplugFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
	}

	synchronized void start()
	{
		context.registerReceiver(permReceiver, permFilter);
		context.registerReceiver(hotplugReceiver, hotplugFilter);
	}

	synchronized void stop()
	{
		context.unregisterReceiver(hotplugReceiver);
		context.unregisterReceiver(permReceiver);
	}

	private void permissionCallback(UsbDevice dev, boolean granted)
	{
		Log.d("UsbEventMonitor", "permission " +
		      (granted ? "granted" : "denied") + " for device " + dev);
		addRemoveDevice(dev, !granted);
	}

	private void attachCallback(UsbDevice dev)
	{
		Log.d("UsbEventMonitor", "device " + dev + "added");
		if (manager.hasPermission(dev))
			addRemoveDevice(dev, false);
	}

	private void detachCallback(UsbDevice dev)
	{
		Log.d("UsbEventMonitor", "device " + dev + "removed");
		addRemoveDevice(dev, true);
	}

	private synchronized void addRemoveDevice(UsbDevice dev, boolean removed)
	{
		listener.onUsbDeviceAction(dev.getDeviceName(), removed);
	}
}
