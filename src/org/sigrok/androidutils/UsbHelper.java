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

package org.sigrok.androidutils;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import java.util.HashMap;

public final class UsbHelper
{
	private static UsbManager manager;
	private static Context context;
	private static UsbEventMonitor eventMonitor;

	public static void setContext(Context ctx)
	{
		context = ctx;
		if (ctx == null)
			manager = null;
		else
			manager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
	}

	private static int open(UsbManager manager, String name, int mode)
	{
		if (manager == null) {
			Log.w("UsbHelper", "no manager");
			return -1;
		}
		HashMap<String,UsbDevice> devlist = manager.getDeviceList();
		UsbDevice dev = (devlist == null ? null : devlist.get(name));
		if (dev == null)
			return -1;
		if (!manager.hasPermission(dev))
			return -1;
		UsbDeviceConnection conn = manager.openDevice(dev);
		return (conn == null ? -1 : conn.getFileDescriptor());
	}

	private static synchronized void startEventMonitor(Context context, UsbManager manager, UsbEventListener listener)
	{
		if (eventMonitor != null) {
		    eventMonitor.stop();
		    eventMonitor = null;
		}
		if (context == null) {
			Log.w("UsbHelper", "no context");
			return;
		}
		if (manager == null) {
			Log.w("UsbHelper", "no manager");
			return;
		}
		eventMonitor = new UsbEventMonitor(context, manager, listener);
		eventMonitor.start();
	}

	private static synchronized void stopEventMonitor(Context context)
	{
		if (eventMonitor != null) {
		    eventMonitor.stop();
		    eventMonitor = null;
		}
	}

	private static String[] scanDevices(UsbManager manager)
	{
		if (manager == null) {
			Log.w("UsbHelper", "no manager");
			return null;
		}
		HashMap<String,UsbDevice> devlist = manager.getDeviceList();
		if (devlist == null)
			return null;
		String[] list = devlist.keySet().toArray(new String[devlist.size()]);
		return list;
	}

	public static int open(String name, int mode)
	{
		try {
			return open(manager, name, mode);
		} catch (Exception e) {
			Log.w("UsbHelper", "caught exception " + e);
			return -1;
		}
	}

	public static void startEventMonitor(UsbEventListener listener)
	{
		try {
			startEventMonitor(context, manager, listener);
		} catch (Exception e) {
			Log.w("UsbHelper", "caught exception " + e);
		}
	}

	public static void stopEventMonitor()
	{
		try {
			stopEventMonitor(context);
		} catch (Exception e) {
			Log.w("UsbHelper", "caught exception " + e);
		}
	}

	public static String[] scanDevices()
	{
		try {
			return scanDevices(manager);
		} catch (Exception e) {
			Log.w("UsbHelper", "caught exception " + e);
			return null;
		}
	}
}

