package aurora.sqlje.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import aurora.sqlje.ast.AstTransform;
import aurora.sqlje.parser.ParsedSource;
import aurora.sqlje.parser.SqljeParser;

public class CliMain {
	static boolean verbos = false;
	static String charset = "UTF-8";
	static List<String> exts = Arrays.asList(new String[] { "sqlje" });

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options opts = new Options();
		opts.addOption(OptionBuilder.withArgName("charset").hasArg()
				.withDescription("charset of SQLJE source file").create('c'));
		opts.addOption(OptionBuilder.withArgName("file").hasArg()
				.withDescription("path of SQLJE source file").create('f'));
		opts.addOption(OptionBuilder.withArgName("directory").hasArg()
				.withDescription("directory of SQLJE source").create('d'));
		opts.addOption(OptionBuilder.withArgName("directory").hasArg()
				.withDescription("directory of output").create('o'));
		opts.addOption(OptionBuilder.withArgName("ext1,ext2..").hasArg()
				.withDescription("SQLJE file extentions").create('e'));
		opts.addOption(OptionBuilder.withDescription("verbos mode").create('v'));
		if (args.length == 0) {
			HelpFormatter helpFormat = new HelpFormatter();
			helpFormat.printHelp("SQLJE Translator", opts);
			return;
		}
		CommandLineParser parser = new GnuParser();
		CommandLine cl = parser.parse(opts, args, true);
		verbos = cl.hasOption('v');

		if (cl.hasOption('c')) {
			charset = cl.getOptionValue('c');
		}
		if (cl.hasOption('e')) {
			exts = Arrays.asList(cl.getOptionValue('e').toLowerCase()
					.split(","));
		}
		if (cl.hasOption('f')) {
			String file = cl.getOptionValue('f');
			File f = new File(file);
			if (!(f.exists() && f.isFile())) {
				throw new FileNotFoundException(
						"file not exists(or not a file)[" + file + "]");
			}
			String dir = f.getParent();
			if (cl.hasOption('o')) {
				dir = cl.getOptionValue('o');
			}

			File javaFile = new File(dir, getNameWithoutExt(f) + ".java");
			javaFile.getParentFile().mkdirs();
			translate(f, javaFile, charset);
		} else if (cl.hasOption('d')) {
			final File dir = new File(cl.getOptionValue('d'));
			if (!(dir.exists() && dir.isDirectory())) {
				throw new FileNotFoundException(
						"directory not exists(or not a directory)["
								+ dir.getAbsolutePath() + "]");
			}
			final String out = cl.getOptionValue('o');
			visit(dir, new Filter() {

				@Override
				void process(File f) throws Exception {
					if (exts.contains(getFileExt(f))) {
						File javaFile = null;
						if (out == null)
							javaFile = new File(f.getParentFile(),
									getNameWithoutExt(f) + ".java");
						else {
							File baseDir = new File(out);
							String rel = f.getAbsolutePath().substring(
									dir.getAbsolutePath().length());
							javaFile = new File(baseDir, rel);
							javaFile = new File(javaFile.getParentFile(),
									getNameWithoutExt(javaFile) + ".java");
							javaFile.getParentFile().mkdirs();
						}
						translate(f, javaFile, charset);
					}
				}
			});
		}
	}

	private static void translate(File sqljeFile, File javaFile, String charset)
			throws Exception {
		long ts = System.currentTimeMillis();
		try {
			debug("+begin translate " + sqljeFile.getAbsolutePath());
			int len = (int) sqljeFile.length();
			byte[] b = new byte[len];
			FileInputStream fis = new FileInputStream(sqljeFile);
			debug("  reading file...");
			len = fis.read(b);
			fis.close();
			String sqljeSrc = new String(b, 0, len, charset);
			SqljeParser sqljeParser = new SqljeParser(sqljeSrc);
			debug("  parsing SQLJE source...");
			ParsedSource ps = sqljeParser.parse();
			AstTransform astTransform = new AstTransform(ps);
			debug("  transforming ast...");
			String javaSrc = astTransform.tranform();
			FileOutputStream fos = new FileOutputStream(javaFile);
			debug("  writing file...");
			fos.write(javaSrc.getBytes("UTF-8"));
			fos.close();
			info("success " + sqljeFile.getAbsolutePath());
			info("    --> " + javaFile.getAbsolutePath());
		} catch (Exception e) {
			info("error " + sqljeFile.getAbsolutePath() + " : "
					+ e.getMessage());
			throw e;
		} finally {
			debug("-end (" + (System.currentTimeMillis() - ts) + "ms)");
		}
	}

	private static void visit(File rootFir, Filter filter) throws Exception {
		for (File f : rootFir.listFiles()) {
			if (f.isFile())
				filter.process(f);
			else if (f.isDirectory()) {
				visit(f, filter);
			}
		}
	}

	private static String getNameWithoutExt(File f) {
		String n = f.getName();
		int lastDotIdx = n.lastIndexOf('.');
		if (lastDotIdx == -1)
			return n;
		return n.substring(0, lastDotIdx);
	}

	private static String getFileExt(File f) {
		String n = f.getName();
		int lastDotIdx = n.lastIndexOf('.');
		if (lastDotIdx == -1)
			return null;
		return n.substring(lastDotIdx + 1).toLowerCase();
	}

	private static void debug(String str) {
		if (verbos) {
			System.out.println(str);
		}
	}

	private static void info(String str) {
		if (verbos)
			str = " " + str;
		System.out.println(str);
	}

	static class Filter {
		void process(File f) throws Exception {

		}
	}

}