/*
 * This file is part of the sigrok-androidutils project.
 *
 * Copyright (C) 2014 Marcus Comstedt <marcus@mc.pp.se>
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
/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sigrok.androidutils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class UsbSupplicant
{
	private static final String ACTION_USB_PERMISSION =
	"org.sigrok.androidutils.USB_PERMISSION";

	protected final Context context;
	protected final UsbManager manager;
	private final BroadcastReceiver permReceiver;
	private final BroadcastReceiver hotplugReceiver;
	private final IntentFilter permFilter;
	private final IntentFilter hotplugFilter;
	private final Vector<DeviceFilter> deviceFilters;

	// The code in the following inner class is taken from AOSP,
	// which is licensed under the Apache License, Version 2.0.
	private static class DeviceFilter {
		// USB Vendor ID (or -1 for unspecified)
		public final int mVendorId;
		// USB Product ID (or -1 for unspecified)
		public final int mProductId;
		// USB device or interface class (or -1 for unspecified)
		public final int mClass;
		// USB device subclass (or -1 for unspecified)
		public final int mSubclass;
		// USB device protocol (or -1 for unspecified)
		public final int mProtocol;

		public DeviceFilter(int vid, int pid, int clasz, int subclass, int protocol) {
			mVendorId = vid;
			mProductId = pid;
			mClass = clasz;
			mSubclass = subclass;
			mProtocol = protocol;
		}

		public DeviceFilter(UsbDevice device) {
			mVendorId = device.getVendorId();
			mProductId = device.getProductId();
			mClass = device.getDeviceClass();
			mSubclass = device.getDeviceSubclass();
			mProtocol = device.getDeviceProtocol();
		}

		public static DeviceFilter read(XmlPullParser parser)
				throws XmlPullParserException, IOException {
			int vendorId = -1;
			int productId = -1;
			int deviceClass = -1;
			int deviceSubclass = -1;
			int deviceProtocol = -1;

			int count = parser.getAttributeCount();
			for (int i = 0; i < count; i++) {
				String name = parser.getAttributeName(i);
				// All attribute values are ints
				int value = Integer.parseInt(parser.getAttributeValue(i));

				if ("vendor-id".equals(name)) {
					vendorId = value;
				} else if ("product-id".equals(name)) {
					productId = value;
				} else if ("class".equals(name)) {
					deviceClass = value;
				} else if ("subclass".equals(name)) {
					deviceSubclass = value;
				} else if ("protocol".equals(name)) {
					deviceProtocol = value;
				}
			}
			return new DeviceFilter(vendorId, productId,
					deviceClass, deviceSubclass, deviceProtocol);
		}

		private boolean matches(int clasz, int subclass, int protocol) {
			return ((mClass == -1 || clasz == mClass) &&
					(mSubclass == -1 || subclass == mSubclass) &&
					(mProtocol == -1 || protocol == mProtocol));
		}

		public boolean matches(UsbDevice device) {
			if (mVendorId != -1 && device.getVendorId() != mVendorId)
				return false;
			if (mProductId != -1 && device.getProductId() != mProductId)
				return false;

			// Check device class/subclass/protocol.
			if (matches(device.getDeviceClass(), device.getDeviceSubclass(),
					device.getDeviceProtocol()))
				return true;

			// If device doesn't match, check the interfaces.
			int count = device.getInterfaceCount();
			for (int i = 0; i < count; i++) {
				UsbInterface intf = device.getInterface(i);
				if (matches(intf.getInterfaceClass(), intf.getInterfaceSubclass(),
						intf.getInterfaceProtocol()))
					return true;
			}

			return false;
		}

		@Override
		public String toString() {
			return "DeviceFilter[mVendorId=" + mVendorId + ",mProductId=" + mProductId +
					",mClass=" + mClass + ",mSubclass=" + mSubclass +
					",mProtocol=" + mProtocol + "]";
		}
	}

	public UsbSupplicant(Context ctx, int device_filter_resource)
	{
		context = ctx;
		manager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
		permReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (ACTION_USB_PERMISSION.equals(action)) {
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
		permFilter = new IntentFilter(ACTION_USB_PERMISSION);
		hotplugFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		hotplugFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		deviceFilters = new Vector<DeviceFilter>();
		addDeviceFilters(ctx.getResources(), device_filter_resource);
	}

	private void addDeviceFilters(Resources res, int res_id)
	{
		XmlResourceParser parser = res.getXml(res_id);
		if (parser == null) {
			Log.w("UsbSupplicant", "Unable to get device filter resource");
			return;
		}
		deviceFilters.clear();
		try {
			while (parser.next() != XmlPullParser.END_DOCUMENT) {
				if (parser.getEventType() == XmlPullParser.START_TAG) {
					if ("usb-device".equals(parser.getName()))
						deviceFilters.add(DeviceFilter.read(parser));
				}
			}
		} catch (IOException e) {
			Log.wtf("UsbSupplicant",
				"Failed to parse device filter resource", e);
		} catch (XmlPullParserException e) {
			Log.wtf("UsbSupplicant",
				"Failed to parse device filter resource", e);
		}
	}

	protected boolean interesting(UsbDevice dev)
	{
		if (dev == null)
			return false;

		for (DeviceFilter f : deviceFilters)
			if (f.matches(dev))
				return true;

		return false;
	}

	protected void askFor(UsbDevice dev)
	{
		manager.requestPermission(dev, PendingIntent.getBroadcast(context, 0,
			new Intent(ACTION_USB_PERMISSION), 0));
	}

	public void start()
	{
		context.registerReceiver(permReceiver, permFilter);
		context.registerReceiver(hotplugReceiver, hotplugFilter);
		HashMap<String,UsbDevice> devlist = manager.getDeviceList();
		for (UsbDevice dev : devlist.values()) {
			if (interesting(dev) && !manager.hasPermission(dev)) {
				Log.d("UsbSupplicant", "found interesting device " + dev);
				askFor(dev);
			}
		}
	}

	public void stop()
	{
		context.unregisterReceiver(hotplugReceiver);
		context.unregisterReceiver(permReceiver);
	}

	protected void permissionCallback(UsbDevice dev, boolean granted)
	{
		Log.d("UsbSupplicant", "permission " +
				(granted ? "granted" : "denied") + " for device " + dev);
	}

	protected void attachCallback(UsbDevice dev)
	{
		if (interesting(dev) && !manager.hasPermission(dev))
			askFor(dev);
	}

	protected void detachCallback(UsbDevice dev)
	{
	}
}
