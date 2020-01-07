package org.burningwave.core.classes.hunter;


import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.burningwave.core.classes.ClassHelper;
import org.burningwave.core.classes.JavaClass;
import org.burningwave.core.classes.MemberFinder;
import org.burningwave.core.classes.hunter.SearchCriteriaAbst.TestContext;
import org.burningwave.core.common.Streams;
import org.burningwave.core.common.Strings;
import org.burningwave.core.concurrent.ParallelTasksManager;
import org.burningwave.core.function.ThrowingRunnable;
import org.burningwave.core.function.ThrowingSupplier;
import org.burningwave.core.io.ByteBufferInputStream;
import org.burningwave.core.io.FileInputStream;
import org.burningwave.core.io.FileOutputStream;
import org.burningwave.core.io.FileSystemHelper;
import org.burningwave.core.io.FileSystemHelper.Scan;
import org.burningwave.core.io.PathHelper;
import org.burningwave.core.io.StreamHelper;
import org.burningwave.core.io.ZipInputStream;

public class ClassPathHunter extends CacherHunter<Class<?>, File, ClassPathHunter.SearchContext, ClassPathHunter.SearchResult> {
	Collection<File> temporaryFiles;
	private ClassPathHunter(
		Supplier<ByteCodeHunter> byteCodeHunterSupplier,
		Supplier<ClassHunter> classHunterSupplier,
		FileSystemHelper fileSystemHelper, 
		PathHelper pathHelper,
		StreamHelper streamHelper,
		ClassHelper classHelper,
		MemberFinder memberFinder
	) {
		super(
			byteCodeHunterSupplier,
			classHunterSupplier,
			fileSystemHelper,
			pathHelper,
			streamHelper,
			classHelper,
			memberFinder,
			(initContext) -> SearchContext._create(fileSystemHelper, streamHelper, initContext),
			(context) -> new ClassPathHunter.SearchResult(context)
		);
		temporaryFiles = ConcurrentHashMap.newKeySet();
	}
	
	public static ClassPathHunter create(Supplier<ByteCodeHunter> byteCodeHunterSupplier, Supplier<ClassHunter> classHunterSupplier, FileSystemHelper fileSystemHelper, PathHelper pathHelper, StreamHelper streamHelper,
		ClassHelper classHelper, MemberFinder memberFinder
	) {
		return new ClassPathHunter(byteCodeHunterSupplier, classHunterSupplier, fileSystemHelper, pathHelper, streamHelper, classHelper, memberFinder);
	}
	
	@Override
	void loadCache(SearchContext context, Collection<String> paths) {
		context.deleteTemporaryFiles(false);
		super.loadCache(context, paths);		
	}
	
	@Override
	<S extends SearchCriteriaAbst<S>> void iterateAndTestItemsForPath(SearchContext context, String path, Map<Class<?>, File> itemsForPath) {
		for (Entry<Class<?>, File> cachedItemAsEntry : itemsForPath.entrySet()) {
			TestContext<S> testContext = testCachedItem(context, path, cachedItemAsEntry.getKey(), cachedItemAsEntry.getValue());
			if(testContext.getResult()) {
				addCachedItemToContext(context, testContext, path, cachedItemAsEntry);
				break;
			}
		}
	}
	
	@Override
	<S extends SearchCriteriaAbst<S>> TestContext<S> testCachedItem(SearchContext context, String path, Class<?> cls, File file) {
		return context.testCriteria(cls);
	}
	
	@Override
	void retrieveItemFromFileInputStream(
		SearchContext context, 
		TestContext<SearchCriteria> criteriaTestContext,
		Scan.ItemContext<FileInputStream> scanItemContext,
		JavaClass javaClass
	) {
		String classPath = Strings.Paths.uniform(scanItemContext.getInput().getFile().getAbsolutePath());
		classPath = classPath.substring(
			0, classPath.lastIndexOf(
				javaClass.getPath(), classPath.length() -1
			)
		-1);	
		File classPathAsFile = new File(classPath);
		context.addItemFound(
			scanItemContext.getBasePathAsString(),
			context.loadClass(javaClass.getName()),
			classPathAsFile
		);
	}

	@Override
	void retrieveItemFromZipEntry(
		SearchContext context,
		TestContext<SearchCriteria> criteriaTestContext,
		Scan.ItemContext<ZipInputStream.Entry> scanItemContext,
		JavaClass javaClass
	) {
		File fsObject = null;
		ZipInputStream.Entry zipEntry = scanItemContext.getInput();
		if (zipEntry.getName().equals(javaClass.getPath())) {
			fsObject = new File(zipEntry.getZipInputStream().getName());
			if (!fsObject.exists()) {
				fsObject = extractLibrary(context, zipEntry);
			}
			if (!context.getCriteria().hasNoPredicate()) {
				scanItemContext.getParent().setDirective(Scan.Directive.STOP_ITERATION);
			}
		} else {
			fsObject = extractClass(context, zipEntry, javaClass);
		}
		context.addItemFound(scanItemContext.getBasePathAsString(), context.loadClass(javaClass.getName()), fsObject);
	}
	
	File extractClass(ClassPathHunter.SearchContext context, ZipInputStream.Entry zipEntry, JavaClass javaClass) {
		String libName = Strings.Paths.uniform(zipEntry.getZipInputStream().getName());
		libName = libName.substring(libName.lastIndexOf("/", libName.length()-2)+1, libName.lastIndexOf("/"));
		return copyToTemporaryFolder(
			context, zipEntry.toByteBuffer(),
			"classes", libName, javaClass.getPackagePath(), javaClass.getClassName() + ".class"
		);
	}

	
	File extractLibrary(ClassPathHunter.SearchContext context, ZipInputStream.Entry zipEntry) {
		String libName = Strings.Paths.uniform(zipEntry.getZipInputStream().getName());
		libName = libName.substring(libName.lastIndexOf("/", libName.length()-2)+1, libName.lastIndexOf("/"));
		return copyToTemporaryFolder(
			context, zipEntry.getZipInputStream().toByteBuffer(), 
			"lib", null, null, libName
		);
	}
	
	
	File getMainFolder(ClassPathHunter.SearchContext context, String folderName) {
		File mainFolder = context.temporaryFiles.stream().filter((file) -> file.getName().contains(folderName)).findFirst().orElse(null);
		if (mainFolder == null) {
			mainFolder = createTemporaryFolder(context, folderName);
		}
		return mainFolder;
	}
	
	
	File createTemporaryFolder(ClassPathHunter.SearchContext context, String folderName) {
		return ThrowingSupplier.get(() ->{
			File tempFile = File.createTempFile("_BW_TEMP_", "_" + folderName);
			tempFile.setWritable(true);
			tempFile.delete();
			tempFile.mkdir();
			context.temporaryFiles.add(tempFile);
			return tempFile;
		});
	}
	
	
	File copyToTemporaryFolder(ClassPathHunter.SearchContext context, ByteBuffer buffer, String mainFolderName, String libName, String packageFolders, String fileName) {
		File toRet = getMainFolder(context, mainFolderName);
		if (libName != null && !libName.isEmpty()) {
			toRet = new File(toRet.getAbsolutePath(), libName);
			toRet.mkdirs();
		}
		File destinationFilePath = toRet;
		if (packageFolders != null && !packageFolders.isEmpty()) {
			destinationFilePath = new File(toRet.getAbsolutePath(), packageFolders + "/");
			destinationFilePath.mkdirs();
		}
		File destinationFile =  new File(destinationFilePath.getAbsolutePath() + "/" + fileName);
		if (!destinationFile.exists()) {
			ThrowingRunnable.run(() -> destinationFile.createNewFile());
			context.tasksManager.addTask(() -> {
				try (FileOutputStream output = FileOutputStream.create(destinationFile, true)){
					Streams.copy(new ByteBufferInputStream(buffer), output);
				}
			});
		}
		if (libName == null || libName.isEmpty()) {
			toRet = destinationFile;
		}
		return toRet;
	}
	
	@Override
	public void close() {
		fileSystemHelper.deleteFiles(temporaryFiles);
		temporaryFiles = null;
		fileSystemHelper = null;
		super.close();
	}
	
	public static class SearchContext extends org.burningwave.core.classes.hunter.SearchContext<Class<?>, File> {
		ParallelTasksManager tasksManager;
		Collection<File> temporaryFiles;
		boolean deleteTemporaryFilesOnClose;
		
		SearchContext(FileSystemHelper fileSystemHelper, StreamHelper streamHelper, InitContext initContext) {
			super(fileSystemHelper, streamHelper, initContext);
			this.temporaryFiles = ConcurrentHashMap.newKeySet();
			ClassFileScanConfiguration scanConfig = initContext.getClassFileScanConfiguration();
			this.tasksManager = ParallelTasksManager.create(scanConfig.maxParallelTasksForUnit);
			deleteTemporaryFilesOnClose = getCriteria().deleteFoundItemsOnClose;
		}		

		public void deleteTemporaryFiles(boolean value) {
			deleteTemporaryFilesOnClose = value;			
		}

		static SearchContext _create(FileSystemHelper fileSystemHelper, StreamHelper streamHelper, InitContext initContext) {
			return new SearchContext(fileSystemHelper, streamHelper,  initContext);
		}
		
		@Override
		public void close() {
			if (deleteTemporaryFilesOnClose) {
				itemsFoundFlatMap.values().removeAll(temporaryFiles);
				fileSystemHelper.deleteFiles(temporaryFiles);
			}
			temporaryFiles = null;
			tasksManager.close();
			super.close();
		}
	}
	
	public static class SearchResult extends org.burningwave.core.classes.hunter.SearchResult<Class<?>, File> {

		public SearchResult(SearchContext context) {
			super(context);
		}
		
	}
}
