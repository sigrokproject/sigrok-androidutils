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

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class SigrokApplication extends Application {

	private static final String JNI_LIBS_RESOURCE_ID_META =
	    "org.sigrok.androidutils.jni_libs_resource_id";

	public SigrokApplication()
	{
	}

	public static void initSigrok(Context context)
	{
		ApplicationInfo appInfo = context.getApplicationInfo();
		Environment.initEnvironment(appInfo.sourceDir);
		UsbHelper.setContext(context);
		try {
			appInfo = context.getPackageManager().
				getApplicationInfo(context.getPackageName(),
				PackageManager.GET_META_DATA);
		} catch (PackageManager.NameNotFoundException exc) {
		}
		if (appInfo.metaData != null &&
				appInfo.metaData.containsKey(JNI_LIBS_RESOURCE_ID_META)) {
			int resId = appInfo.metaData.getInt(JNI_LIBS_RESOURCE_ID_META);
			String[] libs = context.getResources().getStringArray(resId);
			int numLibs = libs.length;
			for (int i = 0; i < numLibs; i++) {
				String libName = libs[i];
				System.loadLibrary(libName);
			}
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		initSigrok(getApplicationContext());
	}
}
