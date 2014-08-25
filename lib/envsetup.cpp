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

#include "libsigrokandroidutils.h"
#include "libsigrokandroidutils-internal.h"

SRAU_API void srau_init_environment(void)
{
	JNIEnv* env;

	int attach_mode = srau_get_java_env(&env);

	if (attach_mode < 0)
		return;

	jclass envc = srau_get_environment_class(env);
	if (!envc)
		return;

	jmethodID getEnv = env->GetStaticMethodID(envc, "getEnvironment",
						  "()[Ljava/lang/String;");
	jobjectArray envs =
		(jobjectArray)env->CallStaticObjectMethod(envc, getEnv);
	jsize i, envn = env->GetArrayLength(envs);
	for (i=0; i<envn; i+=2) {
		jstring key = (jstring)env->GetObjectArrayElement(envs, i);
		jstring value = (jstring)env->GetObjectArrayElement(envs, i+1);
		const char *utfkey = env->GetStringUTFChars(key, 0);
		const char *utfvalue = env->GetStringUTFChars(value, 0);
		setenv(utfkey, utfvalue, 1);
		env->ReleaseStringUTFChars(value, utfvalue);
		env->ReleaseStringUTFChars(key, utfkey);
		env->DeleteLocalRef(value);
		env->DeleteLocalRef(key);
	}
	env->DeleteLocalRef(envs);
	env->DeleteLocalRef(envc);

	srau_unget_java_env(attach_mode);
}
