/*
 * This file is part of the sigrok project.
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

static JavaVM *g_jvm = NULL;
static jclass g_environment_class = 0;

SRAU_PRIV int srau_get_java_env(JNIEnv **env)
{
	jint st;

	if (g_jvm == NULL) {
		return -1;
	}

	st = g_jvm->GetEnv((void **)env, JNI_VERSION_1_6);

	if (st == JNI_EDETACHED) {
		st = g_jvm->AttachCurrentThread(env, NULL);
		if (st == JNI_OK)
			return 1;
	}

	return (st == JNI_OK? 0 : -1);
}

SRAU_PRIV void srau_unget_java_env(int mode)
{
	if (mode == 1) {
		g_jvm->DetachCurrentThread();
	}
}

SRAU_PRIV jclass srau_get_environment_class(JNIEnv *env)
{
	if (env && g_environment_class) {
		return (jclass)env->NewLocalRef(g_environment_class);
	} else {
		return 0;
	}
}

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv* env;

	(void)reserved;

        if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
                return -1;
        }

        jclass envc = env->FindClass("org/sigrok/androidutils/Environment");
	if (envc) {
		g_environment_class = (jclass)env->NewGlobalRef(envc);
		env->DeleteLocalRef(envc);
	}

	g_jvm = vm;

	return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
	JNIEnv* env;

	(void)reserved;

	g_jvm = NULL;

        if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
                return;
        }

	if (g_environment_class) {
		env->DeleteGlobalRef(g_environment_class);
		g_environment_class = 0;
	}
}
