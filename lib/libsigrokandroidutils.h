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

#ifndef LIBSIGROKANDROIDUTILS_LIBSIGROKANDROIDUTILS_H
#define LIBSIGROKANDROIDUTILS_LIBSIGROKANDROIDUTILS_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Use SRAU_API to mark public API symbols, and SRAU_PRIV for private symbols.
 *
 * Variables and functions marked 'static' are private already and don't
 * need SR_PRIV. However, functions which are not static (because they need
 * to be used in other libsigrokandroidutils-internal files) but are also
 * not meant to be part of the public libsigrokandroidutils API, must use
 * SRAU_PRIV.
 *
 * This uses the 'visibility' feature of gcc (requires gcc >= 4.0).
 *
 * Details: http://gcc.gnu.org/wiki/Visibility
 */

/* Marks public libsigrokandroidutils API symbols. */
#define SRAU_API __attribute__((visibility("default")))

/* Marks private, non-public libsigrokandroidutils symbols (not part of the API). */
#define SRAU_PRIV __attribute__((visibility("hidden")))



SRAU_API void srau_init_environment(void);

#ifdef __cplusplus
}
#endif

#endif

