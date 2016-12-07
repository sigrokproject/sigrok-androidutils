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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.selectors.SelectorUtils;

public class CopyLibsTask extends Task implements DynamicAttribute
{
	private static final HashMap<String,String> blacklist;

	static {
		HashMap<String,String> bl = new HashMap<String,String>();
		bl.put("libpcre.so", "libercp.so");
		blacklist = bl;
	}

	private static BuildException buildException(Exception e)
	{
		if (e instanceof BuildException)
			return (BuildException)e;
		else
			return new BuildException(e);
	}

	private static int indexOf(byte[] data, byte v, int start)
	{
		if (data != null) {
			for (int i = start; i < data.length; i++) {
				if (data[i] == v)
					return i;
			}
		}
		return -1;
	}

	private static String fixSoname(String s)
	{
		int l = s.length();
		int i = s.lastIndexOf(".so");

		if (i >= 0 && i < (l - 3))
			s = s.substring(0, i + 3);

		String bl = blacklist.get(s);
		if (bl != null)
			s = bl;

		return s;
	}

	protected class Library implements Comparable<Library>
	{
		protected final File file;
		protected final ElfFile elf;
		protected final HashSet<String> needed;
		protected final Vector<String> rpath;
		protected final TreeSet<Range> fixups;
		protected final String subdir;
		protected String soname, destname;
		protected final HashSet<Library> dependencies;
		protected boolean dependedUpon;

		protected class Range implements Comparable<Range>
		{
			public final long start, end;
			public final byte[] replacement;

			public int compareTo(Range r)
			{
				if (start < r.start)
					return -1;
				else if (start > r.start)
					return 1;
				else if (end < r.end)
					return -1;
				else if (end > r.end)
					return 1;
				else
					return 0;
			}

			public Range(long start, long end, byte[] replacement)
			{
				this.start = start;
				this.end = end;
				this.replacement = replacement;
			}

			public Range(long start, long end)
			{
				this(start, end, null);
			}
		}

		public int compareTo(Library l)
		{
			return destname.compareTo(l.destname);
		}

		protected void addNeeded(String so)
		{
			needed.add(so);
		}

		protected void addRpath(String rp)
		{
			for (String p : rp.split(":")) {
				if (!rpath.contains(p))
					rpath.add(p);
			}
		}

		private String getDynstr(ElfFile.Dynamic d, byte[] s,
					 long base) throws Exception
		{
			int offs = (int)d.d_val;
			int nul = indexOf(s, (byte)0, offs);

			if (nul < 0)
				throw new Exception("Invalid dynamic string");

			String name = new String(s, offs, nul-offs, "US-ASCII");
			offs += base;

			if (d.d_tag == ElfFile.DT_RPATH ||
			    d.d_tag == ElfFile.DT_RUNPATH) {
				// Zap rpath,
				fixups.add(new Range(offs, offs + name.length()));
			} else {
				String fix = fixSoname(name);
				if (fix.length() < name.length()) {
					fixups.add(new Range(offs + fix.length(),
						offs + name.length()));
				}
				if (!fix.equals(name.substring(0, fix.length()))) {
					fixups.add(new Range(offs, offs + fix.length(), fix.getBytes("US-ASCII")));
				}
			}
			return name;
		}

		private void checkDynamic(ElfFile.SectionHeader dynsh,
					ElfFile.SectionHeader strsh) throws Exception
		{
			byte strs[] = new byte[(int)strsh.sh_size];

			elf.read(strsh, strs);

			for (ElfFile.Dynamic d : elf.readDynamic(dynsh)) {
				if (d.d_tag == ElfFile.DT_NULL)
					break;
				else if (d.d_tag == ElfFile.DT_SONAME)
					soname = getDynstr(d, strs, strsh.sh_offset);
				else if (d.d_tag == ElfFile.DT_NEEDED)
					addNeeded(getDynstr(d, strs, strsh.sh_offset));
				else if (d.d_tag == ElfFile.DT_RPATH ||
					 d.d_tag == ElfFile.DT_RUNPATH)
					addRpath(getDynstr(d, strs, strsh.sh_offset));
			}
		}

		private void checkElf() throws Exception
		{
			for (ElfFile.SectionHeader sh : elf.secHeaders) {
				if (sh.sh_type == ElfFile.SHT_DYNAMIC)
					checkDynamic(sh, elf.secHeaders[sh.sh_link]);
			}
		}

		protected File getDestName(File dest)
		{
			File d = (subdir == null? dest : new File(dest, subdir));
			File f = new File(d, destname);
			return f;
		}

		protected void writeTo(File dest) throws IOException
		{
			FileInputStream is = new FileInputStream(file);
			FileOutputStream os = new FileOutputStream(dest);
			byte[] buf = new byte[65536];
			TreeSet<Range> ranges = new TreeSet<Range>(fixups);
			long offs = 0;

			outer: for(;;) {
				long next = offs + buf.length;
				if (!ranges.isEmpty())
					next = ranges.first().start;
				if (next > offs) {
					long chunk = next - offs;
					if (chunk > buf.length)
						chunk = buf.length;
					int r = is.read(buf, 0, (int)chunk);
					if (r < 0)
						break;
					os.write(buf, 0, r);
					offs += r;
					continue;
				}
				while (!ranges.isEmpty() && ranges.first().start <= offs) {
					Range rg = ranges.pollFirst();
					if (rg.end > offs) {
						long chunk = rg.end - offs;
						while (chunk > 0) {
							int slice = (chunk > buf.length ? buf.length : (int)chunk);
							int r = is.read(buf, 0, slice);
							if (r < 0)
								break outer;
							if (r > 0) {
								if (rg.replacement == null)
									Arrays.fill(buf, 0, r, (byte)0);
								else
									System.arraycopy(rg.replacement, (int)(offs-rg.start), buf, 0, r);
								os.write(buf, 0, r);
								chunk -= r;
							}
						}
						offs = rg.end;
					}
				}
			}

			os.close();
			is.close();
		}

		protected Library(File f, String s) throws Exception
		{
			file = f;
			subdir = s;
			elf = new ElfFile(file);
			needed = new HashSet<String>();
			rpath = new Vector<String>();
			fixups = new TreeSet<Range>();
			soname = f.getName();
			dependencies = new HashSet<Library>();
			dependedUpon = false;
			checkElf();
			destname = fixSoname(soname);
		}

		protected Library(Resource r) throws Exception
		{
			this(r.as(FileProvider.class).getFile(),
				new File(r.getName()).getParent());
		}

		public String toString()
		{
			return "Library(" + file + ")";
		}

	};

	protected class Worker
	{
		protected final int machine;
		protected final Queue<Library> workQueue;
		protected final HashMap<String,Library> knownLibs;
		protected final HashSet<Library> processedLibs;
		protected final HashSet<String> allDests;
		protected final Vector<String> rpath;

		protected void addWork(Library l)
		{
			if (l == null)
				return;
			Library kl = knownLibs.get(l.soname);
			if (kl == l)
				return; // Already processed.
			if (kl != null)
				throw new BuildException("Multiple libs with the same soname " + l.soname);
			knownLibs.put(l.soname, l);
			if (allDests.contains(l.destname))
				throw new BuildException("Multiple libs with simplified soname " + l.destname);
			allDests.add(l.destname);
			workQueue.add(l);
		}

		protected void addRpath(Vector<String> rp)
		{
			for (String p : rp) {
				if (!rpath.contains(p))
					rpath.add(p);
			}
		}

		protected void setDependency(Library l1, Library l2)
		{
			if (l2 == null) // Dependency on external lib.
				return;
			l1.dependencies.add(l2);
			l2.dependedUpon = true;
		}

		protected Library findLibInRpath(String s, String subdir)
			throws Exception
		{
			for (String p : rpath) {
				File f = new File(p, s);
				if (f.exists()) {
					Library l = new Library(f, subdir);
					if (l.elf.header.e_machine == machine)
						return l;
				}
			}
			return null;
		}

		protected Library getLibForSoname(String s, String subdir)
			throws Exception
		{
			Library l = knownLibs.get(s);
			if (l != null)
				return l;
			boolean include = false;
			String[] includePatterns = patterns.getIncludePatterns(getProject());
			if (includePatterns != null) {
				for (String patt : includePatterns) {
					if (SelectorUtils.match(patt, s)) {
						include = true;
						break;
					}
				}
			}
			if (!include) {
				String[] excludePatterns = patterns.getExcludePatterns(getProject());
				if (excludePatterns != null) {
					for (String patt : excludePatterns) {
						if (SelectorUtils.match(patt, s))
							return null;
					}
				}
			}
			l = findLibInRpath(s, subdir);
			if (l == null)
				throw new Exception("Library " + s + " not found");
			addWork(l);
			return l;
		}

		protected void process(Library l) throws Exception
		{
			if (processedLibs.contains(l))
				return; // Already processed.
			processedLibs.add(l);
			addRpath(l.rpath);
			for (String need : l.needed)
				setDependency(l, getLibForSoname(need, l.subdir));
		}

		protected Vector<Library> topoSort(HashSet<Library> libs)
		{
			Vector<Library> order = new Vector<Library>();
			for (Library chk : new HashSet<Library>(libs)) {
				if (!chk.dependedUpon)
					libs.remove(chk);
			}
			while (!libs.isEmpty()) {
				HashSet<Library> leafs = new HashSet<Library>();
				for (Library chk : new HashSet<Library>(libs)) {
					if (chk.dependencies.isEmpty())
						leafs.add(chk);
				}
				if (leafs.isEmpty())
					throw new BuildException("Circular dependency found");
				ArrayList<Library> llist = new ArrayList<Library>(leafs);
				Collections.sort(llist);
				order.addAll(llist);
				libs.removeAll(leafs);
				for (Library l : libs)
					l.dependencies.removeAll(leafs);
			}
			return order;
		}

		protected void execute() throws BuildException
		{
			try {
				while (!workQueue.isEmpty())
					process(workQueue.remove());
			} catch (Exception e) {
				throw buildException(e);
			}
			if (property != null) {
				Vector<Library> order =
					topoSort(new HashSet<Library>(processedLibs));
				StringBuilder sb = new StringBuilder();
				for (Library l : order) {
					String name = l.destname;
					if (name.startsWith("lib"))
						name = name.substring(3);
					if (name.endsWith(".so"))
						name = name.substring(0, name.length() - 3);
					sb.append("	<item>");
					sb.append(name);
					sb.append("</item>\n");
				}
				String orderedLibs = sb.toString();
				getProject().setNewProperty(property, orderedLibs);
			}
			for (Library chk : new HashSet<Library>(processedLibs)) {
				File dest = chk.getDestName(destDir);
				if (dest.exists() &&
					dest.lastModified() >= chk.file.lastModified())
					processedLibs.remove(chk);
				dest = dest.getParentFile();
				if (!dest.exists())
					dest.mkdirs();
			}
			if (processedLibs.isEmpty())
				return;
			log("Copying " + processedLibs.size() + " libraries into " + destDir);
			ArrayList<Library> libs = new ArrayList<Library>(processedLibs);
			Collections.sort(libs);
			try {
				for (Library l : libs)
					l.writeTo(l.getDestName(destDir));
			} catch (Exception e) {
				throw buildException(e);
			}
		}

		protected Worker(int mach)
		{
			machine = mach;
			workQueue = new LinkedList<Library>();
			knownLibs = new HashMap<String,Library>();
			processedLibs = new HashSet<Library>();
			allDests = new HashSet<String>();
			rpath = new Vector<String>();
		}

	};

	protected File destDir = null; // The destination directory.
	protected Vector<ResourceCollection> rcs = new Vector<ResourceCollection>();
	protected PatternSet patterns = new PatternSet();
	protected String property = null;
	protected Vector<String> rpath = new Vector<String>();

	public void setTodir(File destDir)
	{
		this.destDir = destDir;
	}

	public void setDynamicAttribute(String name, String value)
	{
		if ("rpath-link".equals(name))
			this.rpath.add(value);
		else
			throw new BuildException("copylibs doesn't support the \"" + name + "\" attribute");
	}

	public void addFileset(FileSet set)
	{
		add(set);
	}

	public void add(ResourceCollection res)
	{
		rcs.add(res);
	}

	public PatternSet.NameEntry createExclude()
	{
		return patterns.createExclude();
	}

	public PatternSet.NameEntry createInclude()
	{
		return patterns.createInclude();
	}

	public void setProperty(String prop)
	{
		property = prop;
	}

	public void execute() throws BuildException
	{
		HashMap<Integer,Worker> workers = new HashMap<Integer,Worker>();
		final int size = rcs.size();

		for (int i = 0; i < size; i++) {
			ResourceCollection rc = rcs.elementAt(i);
			for (Resource r : rc) {
				if (!r.isExists()) {
					String message = "Could not find library "
						+ r.toLongString() + " to copy.";
					throw new BuildException(message);
				}
				Library l;
				try {
					l = new Library(r);
				} catch (Exception e) {
					throw buildException(e);
				}
				Integer m = new Integer(l.elf.header.e_machine);
				Worker w = workers.get(m);
				if (w == null) {
					workers.put(m, (w = new Worker(m.intValue())));
					w.addRpath(rpath);
				}
				w.addWork(l);
			}
		}
		ArrayList<Integer> machines = new ArrayList<Integer>(workers.keySet());
		Collections.sort(machines);
		for (Integer m : machines)
			workers.get(m).execute();
	}
}
