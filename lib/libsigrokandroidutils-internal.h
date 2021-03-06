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

#ifndef LIBSIGROKANDROIDUTILS_LIBSIGROKANDROIDUTILS_INTERNAL_H
#define LIBSIGROKANDROIDUTILS_LIBSIGROKANDROIDUTILS_INTERNAL_H

#include <jni.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

SRAU_PRIV int srau_get_java_env(JNIEnv **env);
SRAU_PRIV void srau_unget_java_env(int mode);
SRAU_PRIV jclass srau_get_environment_class(JNIEnv *env);

#ifdef __cplusplus
}
#endif

#endif

