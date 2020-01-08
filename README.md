**Burningwave Core** is an advanced, free and open source Java library that contains **THE MOST POWERFUL CLASSPATH SCANNER** for criteria based classes search.
It’s possible to search classes by every criteria that your immagination can made by using lambda expressions. **Scan engine is highly optimized using direct allocated ByteBuffers to avoid heap saturation; searches are executed in multithreading context and are not affected by “_the issue of the same class loaded by different classloaders_”** (normally if you try to execute "isAssignableFrom" method on a same class loaded from different classloader it returns false).

**Tested for Java versions ranging from 8 to 13 Burningwave Core is also useful for creating classes during runtime, facilitate the use of reflection and much more...**

Below you will find how to include the library in your projects and a simple code example and in the [wiki](https://github.com/burningwave/core/wiki) you will find more detailed examples.

## Get started

**To include Burningwave Core library in your projects simply use with**:

* **Apache Maven**:
```xml
<dependency>
    <groupId>org.burningwave</groupId>
    <artifactId>core</artifactId>
    <version>3.0.0</version>
</dependency>
```

* **Gradle Groovy**:
```
implementation 'org.burningwave:core:3.0.0'
```

* **Gradle Kotlin**:
```
implementation("org.burningwave:core:3.0.0")
```

* **Scala**:
```
libraryDependencies += "org.burningwave" % "core" % "3.0.0"
```

* **Apache Ivy**:
```
<dependency org="org.burningwave" name="core" rev="3.0.0" />
```

* **Groovy Grape**:
```
@Grapes(
  @Grab(group='org.burningwave', module='core', version='3.0.0')
)
```

* **Leiningen**:
```
[org.burningwave/core "3.0.0"]
```

* **Apache Buildr**:
```
'org.burningwave:core:jar:3.0.0'
```

* **PURL**:
```
pkg:maven/org.burningwave/core@3.0.0
```

## ... And now the code: let's retrieve all classes of the runtime classpath!
```java
import java.util.Collection;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.classes.ClassCriteria;
import org.burningwave.core.classes.hunter.CacheableSearchConfig;
import org.burningwave.core.classes.hunter.ClassHunter;
import org.burningwave.core.classes.hunter.ClassHunter.SearchResult;
import org.burningwave.core.classes.hunter.SearchConfig;
import org.burningwave.core.io.PathHelper;
	
public class Finder {
	
    public Collection<Class<?>> find() {
        ComponentContainer componentConatiner = ComponentContainer.getInstance();
        PathHelper pathHelper = componentConatiner.getPathHelper();
        ClassHunter classHunter = componentConatiner.getClassHunter();
        
        CacheableSearchConfig criteria = SearchConfig.forPaths(
            //Here you can add all absolute path you want:
            //both folders, zip and jar will be recursively scanned.
            //For example you can add: "C:\\Users\\user\\.m2"
            //With the row below the search will be executed on runtime Classpaths
            pathHelper.getMainClassPaths()
		).by(ClassCriteria.create().allThat((cls) -> {
			    return cls.getPackage().getName().matches(".*springframework.*");
			})
		);
        
        SearchResult searchResult = classHunter.findBy(criteria);
        
        return searchResult.getItemsFound();
    }
    
}
```
