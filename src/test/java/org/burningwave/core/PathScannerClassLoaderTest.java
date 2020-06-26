package org.burningwave.core;

import java.util.Arrays;
import java.util.Collections;

import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.PathScannerClassLoader;
import org.junit.jupiter.api.Test;

public class PathScannerClassLoaderTest extends BaseTest {
	
	@Test
	public void getResourcesTestOne() throws ClassNotFoundException {
		testNotEmpty(() -> {
			ComponentSupplier componentSupplier = getComponentSupplier();
			PathScannerClassLoader classLoader = componentSupplier.getPathScannerClassLoader();
			classLoader.scanPathsAndAddAllByteCodesFound(
				Arrays.asList(
					componentSupplier.getPathHelper().getAbsolutePathOfResource("../../src/test/external-resources")
				)
			);
			return Collections.list(classLoader.getResources("META-INF/MANIFEST.MF"));			
		}, true);
	}
	
	@Test
	public void getResourcesTestTwo() throws ClassNotFoundException {
		testNotEmpty(() -> {
			ComponentSupplier componentSupplier = getComponentSupplier();
			PathScannerClassLoader classLoader = componentSupplier.getPathScannerClassLoader();
			classLoader.scanPathsAndAddAllByteCodesFound(
				Arrays.asList(
					componentSupplier.getPathHelper().getAbsolutePathOfResource("../../src/test/external-resources")
				)
			);
			return Collections.list(classLoader.getResources("org/burningwave/RuntimeException.class"));			
		}, true);
	}
	
	@Test
	public void getResourceTestOne() throws ClassNotFoundException {
		testNotNull(() -> {
			ComponentSupplier componentSupplier = getComponentSupplier();
			PathScannerClassLoader classLoader = componentSupplier.getPathScannerClassLoader();
			classLoader.scanPathsAndAddAllByteCodesFound(
				Arrays.asList(
					componentSupplier.getPathHelper().getAbsolutePathOfResource("../../src/test/external-resources")
				)
			);
			return classLoader.getResource("org/burningwave/RuntimeException.class");			
		});
	}
	
	@Test
	public void getResourceTestTwo() throws ClassNotFoundException {
		testNotNull(() -> {
			ComponentSupplier componentSupplier = getComponentSupplier();
			PathScannerClassLoader classLoader = componentSupplier.getPathScannerClassLoader();
			classLoader.scanPathsAndAddAllByteCodesFound(
				Arrays.asList(
					componentSupplier.getPathHelper().getAbsolutePathOfResource("../../src/test/external-resources")
				)
			);
			return classLoader.getResource("META-INF/MANIFEST.MF");			
		});
	}
	
	@Test
	public void getResourceTestThree() throws ClassNotFoundException {
		testNotNull(() -> {
			ComponentSupplier componentSupplier = getComponentSupplier();
			PathScannerClassLoader classLoader = componentSupplier.getPathScannerClassLoader();
			classLoader.scanPathsAndAddAllByteCodesFound(
				Arrays.asList(
					componentSupplier.getPathHelper().getAbsolutePathOfResource("../../src/test/external-resources")
				)
			);
			return classLoader.getResource(".properties");			
		});
	}
}
