#!/bin/sh

libtoolize --install --copy --quiet || exit 1
aclocal || exit 1
automake --add-missing --copy --foreign || exit 1
autoconf || exit 1
