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

package org.sigrok.androidutils.ant;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class ElfFile
{
	public static final int SHT_NULL = 0;
	public static final int SHT_PROGBITS = 1;
	public static final int SHT_SYMTAB = 2;
	public static final int SHT_STRTAB = 3;
	public static final int SHT_RELA = 4;
	public static final int SHT_HASH = 5;
	public static final int SHT_DYNAMIC = 6;
	public static final int SHT_NOTE = 7;
	public static final int SHT_NOBITS = 8;
	public static final int SHT_REL = 9;
	public static final int SHT_SHLIB = 10;
	public static final int SHT_DYNSYM = 11;
	public static final int SHT_INIT_ARRAY = 14;
	public static final int SHT_FINI_ARRAY = 15;
	public static final int SHT_PREINIT_ARRAY = 16;
	public static final int SHT_GROUP = 17;
	public static final int SHT_SYMTAB_SHNDX = 18;

	public static final int DT_NULL = 0;
	public static final int DT_NEEDED = 1;
	public static final int DT_PLTRELSZ = 2;
	public static final int DT_PLTGOT = 3;
	public static final int DT_HASH = 4;
	public static final int DT_STRTAB = 5;
	public static final int DT_SYMTAB = 6;
	public static final int DT_RELA = 7;
	public static final int DT_RELASZ = 8;
	public static final int DT_RELAENT = 9;
	public static final int DT_STRSZ = 10;
	public static final int DT_SYMENT = 11;
	public static final int DT_INIT = 12;
	public static final int DT_FINI = 13;
	public static final int DT_SONAME = 14;
	public static final int DT_RPATH = 15;
	public static final int DT_SYMBOLIC = 16;
	public static final int DT_REL = 17;
	public static final int DT_RELSZ = 18;
	public static final int DT_RELENT = 19;
	public static final int DT_PLTREL = 20;
	public static final int DT_DEBUG = 21;
	public static final int DT_TEXTREL = 22;
	public static final int DT_JMPREL = 23;
	public static final int DT_BIND_NOW = 24;
	public static final int DT_INIT_ARRAY = 25;
	public static final int DT_FINI_ARRAY = 26;
	public static final int DT_INIT_ARRAYSZ = 27;
	public static final int DT_FINI_ARRAYSZ = 28;
	public static final int DT_RUNPATH = 29;
	public static final int DT_FLAGS = 30;
	public static final int DT_ENCODING = 32;
	public static final int DT_PREINIT_ARRAY = 32;
	public static final int DT_PREINIT_ARRAYSZ = 33;

	protected final RandomAccessFile file;
	protected final boolean bit64, little;
	public final Header header;
	public final SectionHeader[] secHeaders;

	protected short swap(short s)
	{
		return (little ? (short)(((s & 0xff) << 8) | ((s >> 8) & 0xff)) : s);
	}

	protected int swap(int i)
	{
		return (little ? (((i & 0xff) << 24) | ((i & 0xff00) << 8) |
				((i >> 8) & 0xff00) | ((i >> 24) & 0xff)) : i);
	}

	protected long swap(long l)
	{
		return (little?	((((long)swap((int)(l & 0xffffffffL))) << 32) |
				(((long)swap((int)((l >> 32) & 0xffffffffL))) & 0xffffffffL)) : l);
	}

	protected short getHalf() throws IOException
	{
		return swap(file.readShort());
	}

	protected int getWord() throws IOException
	{
		return swap(file.readInt());
	}

	protected long getXword() throws IOException
	{
		return swap(file.readLong());
	}

	protected long getAddr() throws IOException
	{
		return (bit64? getXword() : getWord());
	}

	protected long getOff() throws IOException
	{
		return (bit64 ? getXword() : getWord());
	}

	public class Header
	{
		public final byte[] e_ident;
		public final short e_type, e_machine;
		public final int e_version;
		public final long e_entry, e_phoff, e_shoff;
		public final int e_flags;
		public final short e_ehsize, e_phentsize, e_phnum;
		public final short e_shentsize, e_shnum, e_shstrndx;

		private Header(byte[] ident) throws IOException
		{
			e_ident = ident;
			e_type = getHalf();
			e_machine = getHalf();
			e_version = getWord();
			e_entry = getAddr();
			e_phoff = getOff();
			e_shoff = getOff();
			e_flags = getWord();
			e_ehsize = getHalf();
			e_phentsize = getHalf();
			e_phnum = getHalf();
			e_shentsize = getHalf();
			e_shnum = getHalf();
			e_shstrndx = getHalf();
		}
	}

	public class SectionHeader
	{
		public final int sh_name, sh_type;
		public final long sh_flags, sh_addr, sh_offset, sh_size;
		public final int sh_link, sh_info;
		public final long sh_addralign, sh_entsize;

		private SectionHeader() throws IOException
		{
			sh_name = getWord();
			sh_type = getWord();
			sh_flags = (bit64 ? getXword() : getWord());
			sh_addr = getAddr();
			sh_offset = getOff();
			sh_size = (bit64 ? getXword() : getWord());
			sh_link = getWord();
			sh_info = getWord();
			if (bit64) {
				sh_addralign = getXword();
				sh_entsize = getXword();
			} else {
				sh_addralign = getWord();
				sh_entsize = getWord();
			}
		}
	}

	public class Dynamic
	{
		public final long d_tag, d_val;

		private Dynamic() throws IOException
		{
			if (bit64) {
				d_tag = getXword();
				d_val = getXword();
			} else {
				d_tag = getWord();
				d_val = getWord();
			}
		}
	}

	public Dynamic[] readDynamic(SectionHeader sh) throws IOException
	{
		file.seek(sh.sh_offset);
		Dynamic[] dyn = new Dynamic[(int)(sh.sh_size / sh.sh_entsize)];
		for (int i = 0; i < dyn.length; i++)
			dyn[i] = new Dynamic();
		return dyn;
	}

	public void read(SectionHeader sh, byte[] buf) throws Exception
	{
		if (sh.sh_type == SHT_NOBITS || buf.length > sh.sh_size)
			throw new Exception("Illegal read");
		file.seek(sh.sh_offset);
		file.readFully(buf);
	}

	public ElfFile(File f) throws Exception
	{
		file = new RandomAccessFile(f, "r");
		file.seek(0);
		byte[] ident = new byte[16];
		file.readFully(ident);
		if (ident[0] != 0x7f || ident[1] != 'E' ||
			ident[2] != 'L' || ident[3] != 'F')
			throw new Exception("ELF signature not found");
		if (ident[4] == 1)
			bit64 = false;
		else if (ident[4] == 2)
			bit64 = true;
		else
			throw new Exception("Invalid ELF file class");
		if (ident[5] == 1)
			little = true;
		else if (ident[5] == 2)
			little = false;
		else
			throw new Exception("Invalid ELF data encoding");
		header = new Header(ident);
		file.seek(header.e_shoff);
		secHeaders = new SectionHeader[header.e_shnum];
		for (int i = 0; i < header.e_shnum; i++)
			secHeaders[i] = new SectionHeader();
	}
}
